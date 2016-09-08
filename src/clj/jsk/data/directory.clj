(ns jsk.data.directory
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [e85th.commons.ex :as ex]))

(def root-dir-id 1)

(def duplicate-name-ex ::duplicate-name-ex)
(def dir-not-empty-ex ::dir-not-empty-ex)

(s/defn find-by-id :- (s/maybe m/Directory)
  "Finds an directory by id."
  [{:keys [db]} dir-id :- s/Int]
  (db/select-directory-by-id db dir-id))

(def ^{:doc "Same as find-by-id except throws NotFoundException if no such directory."}
  find-by-id! (ex/wrap-not-found find-by-id))

(s/defn find-immediate-children :- [m/DirectoryItem]
  "Finds immediate child directories"
  [{:keys [db]} dir-id :- s/Int]
  (db/select-directory-contents-by-dir-id db dir-id))

(s/defn immediate-child-has-name?
  "Answers true if dir-id has a item-name as a direct child (another dir or job/workflow)."
  [res dir-id :- s/Int item-name :- s/Str]
  (let [names (set (map :name (find-immediate-children res dir-id)))]
    (some? (some names item-name))))

(s/defn validate-unique-name
  "Validates that there is no directory with name nm under parent-id."
  [res dir-id :- s/Int item-name :- s/Str]
  (when (immediate-child-has-name? res dir-id item-name)
    (throw (ex/new-validation-exception duplicate-name-ex (format "%s already exists." item-name)))))

(s/defn validate-create
  "Validates directory creation"
  [res {dir-name :name parent-id :parent-id} :- m/DirectoryCreate]
  (validate-unique-name res parent-id dir-name))

(s/defn create :- m/Directory
  "Creates a new directory."
  [{:keys [db] :as res} new-directory :- m/DirectoryCreate user-id :- s/Int]
  (log/infof "new-directory %s" new-directory)
  (validate-create res new-directory)
  (let [dir-id (db/insert-directory db new-directory user-id)]
    (log/infof "Created new directory with id %s" dir-id)
    (find-by-id res dir-id)))

(s/defn validate-amend
  "Validates directory update."
  [res current :- m/Directory dir-update :- m/DirectoryAmend]
  (let [{:keys [parent-id name]} (merge current dir-update)]
    (validate-unique-name res parent-id name)))

(s/defn amend :- m/Directory
  "Updates and returns the new record."
  [{:keys [db] :as res} dir-id :- s/Int dir-update :- m/DirectoryAmend user-id :- s/Int]
  (let [current (find-by-id res dir-id)]
    (validate-amend res current dir-update)
    (db/update-directory db dir-id dir-update user-id)
    (find-by-id! res dir-id)))

(s/defn validate-delete
  "Allows delete if the directory is empty."
  [res dir-id :- s/Int]
  (when (seq (find-immediate-children res dir-id))
    (throw (ex/new-validation-exception dir-not-empty-ex "Directory is not empty."))))

(s/defn delete
  "Delete a directory."
  [{:keys [db] :as res} dir-id :- s/Int user-id :- s/Int]
  (validate-delete res dir-id)
  (db/delete-directory db dir-id))
