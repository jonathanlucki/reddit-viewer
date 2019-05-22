(ns reddit-viewer.controllers
  (:require
    [ajax.core :as ajax]
    [re-frame.core :as rf]))

(rf/reg-event-db
  :initialize-db
  (fn [_ _]
    {:view  :posts
     :sort-key :score
     :subreddits {"cats" :active "me_irl" :active "uwaterloo" :active "toronto" :active}
     :post-count 24}))

(defn subreddit-active? [subreddit subreddits]
  (= (subreddits subreddit) :active))

(rf/reg-sub
  :view
  (fn [db _]
    (:view db)))

(rf/reg-sub
  :posts
  (fn [db _]
    (:posts db)))

(rf/reg-sub
  :subreddits
  (fn [db _]
    (:subreddits db)))

(rf/reg-sub
  :active-subreddits
  (fn [db _]
    (filter #(subreddit-active? % (:subreddits db)) (keys (:subreddits db)))))

(rf/reg-sub
  :post-count
  (fn [db _]
    (:post-count db)))

(rf/reg-sub
  :active-posts
  (fn [db _]
    (filter #(subreddit-active? (:subreddit %) (:subreddits db)) (:posts db))))


;; -------------------------
;; load posts

(defn find-posts-with-preview [posts]
  (filter #(= (:post_hint %) "image") posts))

(defn prepare-url [subreddits count]
  (str "http://www.reddit.com/r/" (apply str (interpose "+" subreddits)) ".json?sort=new&limit=" count))

(rf/reg-event-db
  :add-posts
  (fn [db [_ posts]]
    (assoc db :posts
              (concat (:posts db)
                      (->> (get-in posts [:data :children])
                           (map :data)
                           (find-posts-with-preview))))))

(rf/reg-fx
  :ajax-get
  (fn [[subreddits post-count handler]]
    (println (prepare-url subreddits post-count))
    (ajax/GET (prepare-url subreddits post-count)
              {:handler         handler
               :response-format :json
               :keywords?       true})))

(rf/reg-event-fx
  :reload-posts
  (fn [cofx [_ _]]
    (let [db (:db cofx)]
      {:db (assoc db :posts [])
       :ajax-get [(filter #(subreddit-active? % (:subreddits db)) (keys (:subreddits db))) (:post-count db) #(rf/dispatch [:add-posts %])]})
    ))


;; -------------------------
;; sort posts

(rf/reg-event-db
  :sort-posts
  (fn [db [_ sort-key]]
    (update db :posts (partial sort-by sort-key >))))


;; -------------------------
;; select view

(rf/reg-event-db
  :select-view
  (fn [db [_ view]]
    (assoc db :view view)))


;; -------------------------
;; select subreddit

(defn new-status [status]
  (if (= :active status)
    :inactive
    :active))

(rf/reg-event-db
  :change-subreddit
  (fn [db [_ subreddit]]
    (let [subreddits (:subreddits db)]
      (assoc db :subreddits
                (assoc subreddits subreddit
                                  (new-status (subreddits subreddit)))))))