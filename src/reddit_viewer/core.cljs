(ns reddit-viewer.core
  (:require
    [reagent.core :as r]
    [ajax.core :as ajax]
    [reddit-viewer.chart :as chart]
    [reddit-viewer.controllers]
    [re-frame.core :as rf]))

(defn display-post [{:keys [permalink subreddit title score num_comments url]}]
  [:div.card.border-dark.w-100
   [:div.card-header
    [:a {:href (str "http://reddit.com/r/" subreddit)}
     (str "/r/" subreddit)]]
   [:div.card-body
    [:h4.card-title
     [:a {:href (str "http://reddit.com" permalink)} title " "]]
    [:div [:span.badge.badge-dark subreddit " score " score]]
    [:img.img-fluid {:width "300px" :src url}]
    [:div [:a {:href (str "http://reddit.com" permalink)} num_comments " comments"]]]])

(defn display-loader [message]
  [:div.d-flex.justify-content-center.align-middle
   [:div.card
    [:div.card-body
     [:div.px-3.py-1
      [:div.row.justify-content-center
       [:div.spinner-border]]
      [:div.row.justify-content-center
       [:strong message]]]]]])

;credit: https://stackoverflow.com/questions/26457225/clojure-partition-list-into-equal-piles
(defn piles [n coll]
  (let [heads (take n (iterate rest coll))]
    (map (partial take-nth n) heads)))

(defn display-posts [posts]
  (if-not (empty? posts)
    ;posts loaded
    [:div.m-3
     [:div.row
      (for [posts-col (piles 3 posts)]
        ^{:key posts-col}
        [:div.col-lg.p-0
         (for [post posts-col]
           ^{:key post}
           [:div.p-1 [display-post post]])])]]
    ;posts not loaded
    [display-loader "Loading..."]))

(defn sort-posts [title sort-key]
  [:button.btn.btn-outline-dark
   {:on-click #(rf/dispatch [:sort-posts sort-key])}
   (str "sort posts by " title)])

(defn reload-button []
  [:button.btn.btn-outline-dark.justify-content-center
   {:on-click #(rf/dispatch [:reload-posts])}
   [:i.material-icons.align-middle "sync"]])

(defn navitem [title view id]
  [:li.nav-item
   {:class-name (when (= id view) "active")}
   [:a.nav-link
    {:href     "#"
     :on-click #(rf/dispatch [:select-view id])}
    title]])

(defn navbar [view]
  [:nav.navbar.navbar-expand-lg.navbar-light.bg-faded
   [:a.navbar-brand {:href "#"}
    [:img.d-inline-block.align-top
     {:src "http://www.stickpng.com/assets/images/5847e9efcef1014c0b5e482e.png"
      :width "32"
      :height "28"
      :alt ""}]
    " Reddit"]
   [:ul.navbar-nav.mr-auto
    [navitem "Posts" view :posts]
    [navitem "Chart" view :chart]]
   [:form.form-inline
    [:div.btn-group
     [sort-posts "score" :score]
     [sort-posts "comments" :num_comments]
     [reload-button]]]])

(defn subreddit-button [subreddit active]
  [:button.btn.btn-outline-dark {:type "button"
                              :className (if active "active")
                              :on-click #(rf/dispatch [:change-subreddit subreddit])}
   (str "/r/" subreddit)])

(defn subreddit-selector [subreddits]
  [:div.d-flex.justify-content-center
   [:div.btn-group {:role "group"}
    (for [subreddit (keys subreddits)]
      ^{:key subreddit}
      [subreddit-button subreddit (= :active (subreddits subreddit))])
    ]])




;; -------------------------
;; Views

(defn home-page []
  (let [view @(rf/subscribe [:view])
        posts @(rf/subscribe [:active-posts])
        subreddits @(rf/subscribe [:subreddits])]
    [:div
     [navbar view]
     [:div
      [subreddit-selector subreddits]
      (case view
        :chart [chart/chart-posts-by-votes]
        :posts [display-posts posts])]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch [:reload-posts])
  (mount-root))

