(ns jsk.data.user
  (:require [datomic.api :as d]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [e85th.commons.ex :as ex]
            [e85th.commons.tel :as tel]
            [e85th.commons.sms :as sms]
            [buddy.hashers :as hashers]
            [jsk.data.models :as m]
            [jsk.data.db :as db]
            [clj-time.core :as t]
            [e85th.backend.core.firebase :as firebase]
            [e85th.backend.core.google-oauth :as google-oauth]
            [e85th.commons.token :as token]
            [e85th.commons.email :as email]
            [e85th.commons.util :as u]
            [e85th.commons.datomic :as datomic]))

(def duplicate-channel-ex ::duplicate-channel-ex)
(def password-required-ex ::password-required-ex)
(def channel-auth-failed-ex ::channel-auth-failed-ex)

(s/defn find-user-all-fields-by-id :- (s/maybe m/UserAllFields)
  "Finds a user entity with the specified id. Raises an exception when not found."
  [{:keys [db]} id :- s/Int]
  (datomic/get-entity-with-attr db id :user/first-name))

(def find-user-all-fields-by-id! (ex/wrap-not-found find-user-all-fields-by-id))

(s/defn find-user-by-id :- (s/maybe m/User)
  [res id]
  (dissoc (find-user-all-fields-by-id res id) :user/password-hash))

(def find-user-by-id! (ex/wrap-not-found find-user-by-id))

(s/defn find-channel-by-id :- (s/maybe m/Channel)
  [{:keys [db]} id :- s/Int]
  (datomic/get-entity-with-attr db id :channel/type))

(def find-channel-by-id! (ex/wrap-not-found find-channel-by-id))

(s/defn find-channel-by-identifier :- (s/maybe m/Channel)
  "Lookup ad channel matching the identifier."
  [{:keys [db]} identifier :- s/Str]
  (db/find-channel-by-identifier db identifier))

(def find-channel-by-identifier! (ex/wrap-not-found find-channel-by-identifier))

(s/defn find-channel-by-type :- (s/maybe m/Channel)
  [{:keys [db]} channel-type :- s/Keyword identifier :- s/Str]
  (db/find-channel-by-type db channel-type identifier))

(def find-channel-by-type! (ex/wrap-not-found find-channel-by-type))

(s/defn find-user-id-by-identifier :- (s/maybe s/Int)
  [{:keys [db]} identifier :- s/Str]
  (db/find-user-id-by-identifier db identifier))

(def find-user-id-by-identifier! (ex/wrap-not-found find-user-id-by-identifier))

(s/defn find-user-all-fields-by-identifier :- (s/maybe m/User)
  [res identifier :- s/Str]
  (->> (find-user-id-by-identifier res identifier)
       (find-user-all-fields-by-id res)))

(def find-user-all-fields-by-identifier! (ex/wrap-not-found find-user-all-fields-by-identifier))

(s/defn find-user-by-identifier :- (s/maybe m/User)
  [res identifier :- s/Str]
  (->> (find-user-id-by-identifier res identifier)
       (find-user-by-id res)))

(def find-user-by-identifier! (ex/wrap-not-found find-user-by-identifier))

(s/defn find-mobile-channel :- (s/maybe m/Channel)
  "Finds an mobile channel for the given mobile phone number."
  [res mobile-nbr :- s/Str]
  (find-channel-by-type res :mobile (tel/normalize mobile-nbr)))

(def find-mobile-channel! (ex/wrap-not-found find-mobile-channel))

(s/defn find-email-channel :- (s/maybe m/Channel)
  "Finds an email channel for the given email address."
  [res email :- s/Str]
  (find-channel-by-type res :email email))

(def find-email-channel! (ex/wrap-not-found find-email-channel))

(s/defn create-channel :- m/Channel
  "Creates a new channel for user with id user-id and returns the Channel record."
  [{:keys [db] :as res} channel :- m/Channel user-id :- s/Int]
  ;; add a fact for the new channel
  ;; add another fact for the channel's relationship to user
  (let [chan-id (d/tempid :db.part/jsk)
        facts [(assoc channel :db/id chan-id)
               {:db/id user-id
                :user/channels chan-id}]
        {:keys [db-after tempids]} @(d/transact (:cn db) facts)]
    (find-channel-by-id res (d/resolve-tempid db-after tempids chan-id))))

(s/defn update-channel :- m/Channel
  "Update the channel attributes and return the updated Channel record."
  [{:keys [db] :as res} channel-id :- s/Int channel :- m/Channel user-id :- s/Int]
  @(d/transact (:cn db) [(assoc channel :db/id channel-id)])
  (find-channel-by-id! res channel-id))

(s/defn rm-channel
  "Removes the channel associated with the user."
  [{:keys [db] :as res} channel-id :- s/Int user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retract channel-id :user/channels channel-id]]))

;; -- New User
(s/defn filter-existing-identifiers :- [s/Str]
  [{:keys [db]} channels :- [m/Channel]]
  (db/filter-existing-identifiers db channels))

(s/defn validate-channel-identifiers
  "Validates that the channel identifier are not already present.
   Throws a validation exception when an identifier exists."
  [res {:keys [channels]} :- m/User]
  (when-let [identifier (first (filter-existing-identifiers res channels))]
    (throw (ex/new-validation-exception duplicate-channel-ex (format "Identifier %s already exists." identifier)))))

(s/defn validate-new-user
  [res {:keys [user/channels user/password] :as new-user} :- m/User]
  (validate-channel-identifiers res new-user)
  (when (some #{:email} (map :channel/type channels))
    (when-not (seq password)
      (throw (ex/new-validation-exception password-required-ex "Passowrd is required for e-mail signup.")))))

(s/defn create-new-user
  [{:keys [db] :as res} {:keys [user/password] :as user} :- m/User creator-id :- s/Int]
  (validate-new-user res user)
  (let [password-hash (hashers/derive (or password (u/uuid)))
        temp-id (d/tempid :db.part/jsk)
        entity (-> user
                   (assoc :db/id temp-id :user/password-hash password-hash)
                   (dissoc :user/password))
        {:keys [db-after tempids]} @(d/transact (:cn db) [entity])]
    (find-user-by-id res (d/resolve-tempid db-after tempids temp-id))))

;; -- user auth
(s/defn find-user-auth
  "Finds roles and permissions for the user specified by user-id."
  [{:keys [db] :as res} user-id :- s/Int]
  (let [{roles :user/roles} (find-user-by-id res user-id)]
    (reduce (fn [ans {:keys [role/name role/permissions]}]
              (-> ans
                  (update-in [:user/roles] conj name)
                  (update-in [:user/permissions] conj (map :permission/name permissions))))
            {:user/roles #{} :user/permissions #{}}
            roles)))

(s/defn find-roles-by-user-id :- #{s/Keyword}
  [res user-id :- s/Int]
  (:user/roles (find-user-auth res user-id)))

(s/defn find-user-ids-by-role-ids :- [s/Int]
  [{:keys [db]} roles :- [s/Keyword]]
  (db/find-user-ids-by-roles db roles))

(s/defn send-mobile-token :- s/Bool
  "Send a mobile token to the user with mobile-nbr. Store in db to verify.
   Throws an exception if the mobile channel doesn't exist for the identifier."
  [{:keys [sms] :as res} req :- m/OneTimePassRequest user-id :- s/Int]
  (let [{mobile-nbr :identifier :keys [token token-expiration]} req
        sms-msg {:to-nbr mobile-nbr :body (:message-body req)}
        {channel-id :db/id} (find-mobile-channel! res mobile-nbr)
        channel-update {:channel/token token
                        :channel/token-expiration token-expiration}]
    (update-channel res channel-id channel-update user-id)
    ;; text the token to the user
    (sms/send sms sms-msg)
    true))

(s/defn user->token :- s/Str
  [{:keys [token-factory]} user :- m/User]
  (assert token-factory)
  (token/data->token token-factory
                     (select-keys user (keys m/TokenUser))))

(s/defn user-id->token :- s/Str
  [res user-id :- s/Int]
  (user->token res (find-user-by-id! res user-id)))

(s/defn token->user :- m/TokenUser
  "Inverse of user->token. Otherwise throws an AuthExceptionInfo."
  [{:keys [token-factory]} token :- s/Str]
  (assert token-factory)
  (token/token->data! token-factory token))

(s/defn ^:private auth-with-token :- s/Int
  "Check the identifier and token combination is not yet expired. Answers with the user-id.
   Throws exceptions if unexpired identifier and token combo is not found."
  [{:keys [db] :as res} identifier :- s/Str token :- s/Str]
  (let [{:keys [db/id channel/token-expiration] :as chan} (find-channel-by-identifier res identifier)]
    (if (and id token-expiration
             (= token (:channel/token chan))
             (t/before? token-expiration (t/now)))
      @(d/transact (:cn db) [[:db/retract id :channel/token]
                             [:db/retract id :channel/token-expiration]
                             [:db/add id :channel/verified-at (t/now)]])
      (throw (ex/new-auth-exception channel-auth-failed-ex "Auth failed.")))))

(s/defn user->auth-response :- m/AuthResponse
  [res user :- m/User]
  {:user user
   :roles (find-roles-by-user-id res (:db/id user))
   :token (user->token res user)})

(s/defn user-id->auth-response :- m/AuthResponse
  [res user-id :- s/Int]
  (user->auth-response res (find-user-by-id! res user-id)))

(s/defn find-email-channels-by-user-id :- [m/Channel]
  "Enumerates all email channels for the user."
  [{:keys [db] :as res} user-id :- s/Int]
  (->> (find-user-by-id! res user-id)
       :user/channels
       (filter (comp (partial = :email) :channel/type))))


(s/defn add-user-roles
  [{:keys [db]} user-id :- s/Int role-ids :- [s/Int] editor-user-id :- s/Int]
  @(d/transact (:cn db) [[:db/add user-id :user/roles role-ids]]))

(s/defn rm-user-roles
  [{:keys [db]} user-id :- s/Int role-ids :- [s/Int] editor-user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retract user-id :user/roles role-ids]]))

(s/defn rm-user
  [{:keys [db]} user-id :- s/Int delete-user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retractEntity user-id]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Authenticate
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti authenticate
  (fn [res user-auth]
    (first (keys user-auth))))

(defmethod authenticate :with-firebase
  [res {:keys [with-firebase]}]
  (let [jwt (:token with-firebase)
        auth-ex-fn (fn [ex]
                     (throw
                      (ex/new-auth-exception :firebase/auth-failed "Firebase Auth Failed" {} ex)))
        {:keys [email]} (firebase/verify-id-token! jwt auth-ex-fn)]

    (assert email "We don't handle anonymous logins a la firebase.")

    (if-let [user (find-user-by-identifier res email)]
      (user->auth-response res user)
      (throw (ex/new-auth-exception :user/no-such-user "No such user.")))))

(defmethod authenticate :with-google
  [res {:keys [with-google]}]
  (let [jwt (:token with-google)
        {:keys [email]} (google-oauth/verify-token jwt)]
    (let [{:keys [email]} (google-oauth/verify-token jwt)]
      (assert email "We don't handle anonymous logins.")
      (if-let [user (find-user-by-identifier res email)]
        (user->auth-response res user)
        (throw (ex/new-auth-exception :user/no-such-user "No such user."))))))

(defmethod authenticate :with-token
  [res {:keys [with-token]}]
  (let [{:keys [identifier token]} with-token
        identifier (cond-> identifier
                     (email/valid? identifier) identity
                     (tel/valid? identifier) tel/normalize)]
    (auth-with-token res identifier token)
    (user->auth-response res (find-user-by-identifier res identifier))))

(defmethod authenticate :with-password
  [res {:keys [with-password]}]
  (let [{:keys [email password]} with-password
        {:keys [user/password-hash] :as user} (find-user-all-fields-by-identifier res email)]

    (when-not (seq password-hash)
      (throw (ex/new-auth-exception :user/invalid-password "Invalid password.")))

    (when-not (hashers/check password password-hash)
      (throw (ex/new-auth-exception :user/invalid-password "Invalid password.")))

    (user->auth-response res user)))
