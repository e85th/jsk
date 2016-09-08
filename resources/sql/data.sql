-- :name select-agent
   select
          a.id
        , a.name
     from agent a
    where (:id-nil? or a.id = :id)
      and (:name-nil? or a.name = :name)

-- :name select-alert
   select
          a.id
        , a.name
        , a.description
     from alert a
    where (:id-nil? or a.id = :id)
      and (:name-nil? or a.name = :name)

-- :name select-schedule
   select
          s.id
        , s.name
        , s.description
        , s.cron_expr     as "cron-expr"
     from schedule s
    where (:id-nil? or s.id = :id)
      and (:name-nil? or s.name = :name)

-- :name select-directory
   select
          d.id
        , d.name
        , d.parent_id as "parent-id"
     from directory d
    where (:id-nil? or d.id = :id)
      and (:name-nil? or d.name = :name)
      and (:parent-id-nil? or d.parent_id = :parent-id)
      and (:root-nil? or d.parent_id is null)


-- :name select-directory-contents
   select
          n.id
        , n.name                      as "name"
        , n.is_enabled                as "is-enabled"
        , nt.name                     as "type"
     from directory_node dn
     join node n
       on dn.node_id = n.id
     join node_type nt
       on n.node_type_id = nt.id
    where dn.directory_id = :directory-id
union all
    select
           d.id
         , d.name
         , true
         , 'dir' as type
      from directory d
     where d.parent_id = :directory-id

-- :name select-job
    select
           j.id
         , n.name
         , n.description
         , n.is_enabled     as "is-enabled"
         , n.tags
         , j.type
         , j.props
         , j.agent_id       as "agent-id"
         , j.behavior_id    as "behavior-id"
         , j.context_key    as "context-key"
         , j.max_retries    as "max-retries"
         , j.max_concurrent as "max-concurrent"
         , j.timeout
      from job  j
      join node n
        on j.node_id = n.id
     where (:id-nil? or j.id = :id)

-- :name select-workflow
    select
           w.id
         , n.name
         , n.description
         , n.tags
         , n.is_enabled    as "is-enabled"
      from workflow w
      join node     n
        on w.node_id = n.id
     where (:id-nil? or w.id = :id)

-- :name select-node-schedule
    select
           ns.id               as id
         , coalesce(j.id,w.id) as "child-id"
         , ns.schedule_id      as "schedule-id"
         , s.name              as "schedule-name"
      from node_schedule ns
      join schedule      s
        on ns.schedule_id = s.id
 left join job           j
        on ns.node_id = j.node_id
 left join workflow      w
        on ns.node_id = w.node_id
     where (:id-nil? or ns.id = :id)
       and (:node-id-nil? or ns.node_id = :node-id)
       and (:schedule-id-nil? or ns.schedule_id = :schedule-id)

-- :name select-node-id->child-id
   select
          n.node_type_id      as "node-type-id"
        , coalesce(j.id,w.id) as id
     from node n
left join job  j
       on n.id = j.node_id
left join workflow w
       on n.id = w.node_id
    where n.id = :node-id
