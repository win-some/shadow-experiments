(ns todomvc.simple
  (:require
    [shadow.experiments.grove :as sg :refer (defc <<)]))

;; simple single file todomvc impl
;; without any db abstraction and directly manipulating atom data
;; not a recommended way to write any actual app but works for really simple stuff

(defonce root-el (js/document.getElementById "app"))

(defc todo-item [{:keys [todo-id todo-text completed?] :as todo}]
  (bind editing
    (sg/env-watch :data-ref [:editing]))

  (bind editing? (= editing todo-id))

  (render
    (<< [:li {:class {:completed completed?
                      :editing editing?}}
         [:div.view
          [:input.toggle {:type "checkbox"
                          :checked completed?
                          :on-change [::toggle-completed! todo-id]}]
          [:label {:on-dblclick [::edit-start! todo-id]} todo-text]
          [:button.destroy {:on-click [::delete! todo-id]}]]

         (when editing?
           (<< [:input#edit.edit {:autofocus true
                                  :on-keydown [::edit-update! todo-id]
                                  :on-blur [::edit-complete! todo-id]
                                  :value todo-text}]))])))

(def filter-options
  [{:label "All" :value :all}
   {:label "Active" :value :active}
   {:label "Completed" :value :completed}])

(defc ui-root []
  (bind {:keys [todos current-filter]}
    (sg/env-watch :data-ref))

  (bind num-total
    (count todos))

  (bind num-active
    (->> todos
         (remove :completed?)
         (count)))

  (bind num-completed
    (- num-total num-active))

  (bind filtered-todos
    (let [filter-fn
          (case current-filter
            :all
            (fn [x] true)
            :active
            #(not (:completed? %))
            :completed
            #(true? (:completed? %)))]

      (->> (vals todos)
           (filter filter-fn)
           (vec))))

  (render
    (<< [:header.header
         [:h1 "todos"]
         [:input.new-todo {:on-keydown [::create-new!]
                           :placeholder "What needs to be done?"
                           :autofocus true}]]

        (when (pos? num-total)
          (<< [:section.main
               [:input#toggle-all.toggle-all
                {:type "checkbox"
                 :on-change [::toggle-all!]
                 :checked false}]
               [:label {:for "toggle-all"} "Mark all as complete"]

               [:ul.todo-list (sg/render-seq filtered-todos :todo-id todo-item)]

               [:footer.footer
                [:span.todo-count
                 [:strong num-active] (if (= num-active 1) " item" " items") " left"]

                [:ul.filters
                 (sg/render-seq filter-options :value
                   (fn [{:keys [label value]}]
                     (<< [:li [:a
                               {:class {:selected (= current-filter value)}
                                :href "#"
                                :on-click [::set-filter! value]}
                               label]])))]


                (when (pos? num-completed)
                  (<< [:button.clear-completed {:on-click [::clear-completed!]} "Clear completed"]))]]))))

  (event ::set-filter! [{:keys [data-ref] :as env} e filter]
    (swap! data-ref assoc :current-filter filter))

  (event ::clear-completed! [{:keys [data-ref] :as env} e]
    (swap! data-ref
      (fn [{:keys [todos] :as db}]
        (assoc db :todos
                  (reduce-kv
                    (fn [t todo-id {:keys [completed?]}]
                      (if-not completed?
                        t
                        (dissoc t todo-id)))
                    todos
                    todos)))))

  (event ::toggle-all! [{:keys [data-ref] :as env} e]
    (let [completed? (-> e .-target .-checked)]
      (swap! data-ref
        (fn [{:keys [todos] :as db}]
          (assoc db :todos
                    (reduce-kv
                      (fn [t todo-id _]
                        (assoc-in t [todo-id :completed?] completed?))
                      todos
                      todos))))))

  (event ::create-new! [{:keys [data-ref] :as env} ^js e]
    (when (= 13 (.-keyCode e))
      (let [input (.-target e)
            text (.-value input)]

        (when (seq text)
          (set! input -value "")

          (swap! data-ref
            (fn [{:keys [id-seq] :as db}]
              (-> db
                  (assoc-in [:todos id-seq] {:todo-id id-seq
                                             :todo-text text
                                             :completed? false})
                  (update :id-seq inc))))))))

  (event ::edit-start! [{:keys [data-ref] :as env} e todo-id]
    (swap! data-ref assoc :editing todo-id))

  (event ::edit-update! [{:keys [data-ref] :as env} e todo-id]
    (case (.-which e)
      13 ;; enter
      (.. e -target (blur))
      27 ;; escape
      (swap! data-ref assoc :editing nil)
      ;; default do nothing
      nil))

  (event ::edit-complete! [{:keys [data-ref] :as env} e todo-id]
    (let [new-text (.. e -target -value)]
      (swap! data-ref
        (fn [db]
          (-> db
              (assoc :editing nil)
              (assoc-in [:todos todo-id :todo-text] new-text))))))

  (event ::toggle-completed! [{:keys [data-ref] :as env} e todo-id]
    (swap! data-ref update-in [:todos todo-id :completed?] not))

  (event ::delete! [{:keys [data-ref] :as env} e todo-id]
    (swap! data-ref update :todos dissoc todo-id)))

(defn ^:dev/after-load start []
  (sg/start ::ui root-el (ui-root)))

;; adding this to the env under :data-ref, env makes it available to the component tree
;; never using this directly otherwise to avoid global state
(defonce data-ref
  (-> {:id-seq 1
       :editing nil
       :todos {}
       :current-filter :all}
      (atom)))

(defn init []
  (sg/init ::ui
    {:data-ref data-ref}
    [])
  (start))