(ns shadow.experiments.grove.protocols)


;; not using record since they shouldn't act as maps
;; also does a bunch of other stuff I don't want
(deftype Ident [entity-type id ^:mutable _hash]
  ILookup
  (-lookup [this key]
    (case key
      :entity-type entity-type
      :id id
      nil))

  IHash
  (-hash [this]
    (if (some? _hash)
      _hash
      (let [x (bit-or 123 (hash id) (hash id))]
          (set! _hash x)
          x)))
  IEquiv
  (-equiv [this ^Ident other]
    (and (instance? Ident other)
         (keyword-identical? entity-type (.-entity-type other))
         (= id (.-id other)))))

(defprotocol IWork
  (work! [this]))

(defprotocol IHandleEvents
  (handle-event! [this ev-map e]))

(defprotocol IScheduleUpdates
  (schedule-update! [this target])
  (unschedule! [this target])
  (run-now! [this action])

  (did-suspend! [this target])
  (did-finish! [this target])

  (run-asap! [this action])
  (run-whenever! [this action]))

(defprotocol IHook
  (hook-init! [this])
  (hook-ready? [this])
  (hook-value [this])
  ;; true-ish return if component needs further updating
  (hook-deps-update! [this val])
  (hook-update! [this])
  (hook-destroy! [this]))

(defprotocol IHookDomEffect
  (hook-did-update! [this did-render?]))

(defprotocol IBuildHook
  (hook-build [this component idx]))

;; just here so that working on components file doesn't cause hot-reload issues
;; with already constructed components
(deftype ComponentConfig
  [component-name
   hooks
   opts
   check-args-fn
   render-deps
   render-fn
   events])

(defprotocol IQueryEngine
  (query-init [this key query config callback])
  (query-destroy [this key])
  ;; FIXME: one shot query that can't be updated later?
  ;; can be done by helper method over init/destroy but engine
  ;; would still do a bunch of needless work
  ;; only had one case where this might have been useful, maybe it isn't worth adding?
  ;; (query-once [this query config callback])
  (transact! [this tx with-return?]))

(defprotocol IStreamEngine
  (stream-init [this env stream-id stream-key opts callback])
  (stream-destroy [this stream-id stream-key])
  (stream-clear [this stream-key]))

