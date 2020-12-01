(ns status-im.ui.screens.multiaccounts.key-storage.views
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require [quo.core :as quo]
            [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.components.react :as react]
            [status-im.multiaccounts.core :as multiaccounts]
            [status-im.ui.screens.multiaccounts.views :as multiaccounts.views]
            [status-im.multiaccounts.key-storage.core :as multiaccounts.key-storage]
            [status-im.ui.components.chat-icon.screen :as chat-icon.screen]
            [status-im.ui.components.topbar :as topbar]
            [status-im.ui.components.toolbar :as toolbar]
            [status-im.ui.components.accordion :as accordion]))

(defn local-topbar [subtitle]
  [topbar/topbar {:title    "Key management"
                  :subtitle subtitle}])

;; Component to render Key and Storage management screen
(defview actions-base [{:keys [next-title next-event]}]
  (letsubs [{:keys [name] :as multiaccount} [:multiaccounts/login]
            {:keys [move-keystore-checked?]} [:multiaccounts/key-storage]]
           [react/view {:flex 1}
            [local-topbar "Choose actions"]
            [accordion/section {:title   name
                                :icon    [chat-icon.screen/contact-icon-contacts-tab
                                          (multiaccounts/displayed-photo multiaccount)]
                                :count   0
                                :content [react/text :acc-text]}]
            [react/view {:flex            1
                         :flex-direction  :column
                         :justify-content :space-between}
             [react/view
              [quo/list-header "Actions"]
              [quo/list-item {:title              "Move keystore file"
                              :subtitle           "Select a new location to save your private key(s)"
                              :subtitle-max-lines 4
                              :accessory          :checkbox
                              :active             move-keystore-checked?
                              :on-press           #(re-frame/dispatch [::multiaccounts.key-storage/move-keystore-checked (not move-keystore-checked?)])}]
              [quo/list-item {:title              "Reset database"
                              :subtitle           "Delete chats, contacts and settings. Required when you’ve lost your password"
                              :subtitle-max-lines 4
                              :disabled           true
                              :active             move-keystore-checked?
                              :accessory          :checkbox}]]
             (when (and next-title next-event)
               [toolbar/toolbar {:show-border? true
                                 :right        [quo/button
                                                {:type     :secondary
                                                 :disabled (not move-keystore-checked?)
                                                 :on-press #(re-frame/dispatch next-event)
                                                 :after    :main-icons/next}
                                                next-title]}])]]))

(defn actions-not-logged-in
  "To be used when the flow is accessed before login, will enter seed phrase next"
  []
  [actions-base {:next-title "Enter seed phrase"
                 :next-event [::multiaccounts.key-storage/enter-seed-pressed]}])

(defn actions-logged-in
  "To be used when the flow is accessed from profile, will choose storage next"
  []
  [actions-base {:next-title "Choose storage"
                 :next-event [::multiaccounts.key-storage/choose-storage-pressed]}])

(defview seed-phrase []
  (letsubs
   [{:keys [seed-word-count seed-shape-invalid?]} [:multiaccounts/key-storage]]
   [react/keyboard-avoiding-view {:flex 1}
    [local-topbar "Enter seed phrase"]
    [multiaccounts.views/seed-phrase-input
     {:on-change-event     [::multiaccounts.key-storage/seed-phrase-input-changed]
      :seed-word-count     seed-word-count
      :seed-shape-invalid? seed-shape-invalid?}]
    [react/text {:style {:color         colors/gray
                         :font-size     14
                         :margin-bottom 8
                         :text-align    :center}}
     (i18n/label :t/multiaccounts-recover-enter-phrase-text)]
    [toolbar/toolbar {:show-border? true
                      :right        [quo/button
                                     {:type     :secondary
                                      :disabled seed-shape-invalid?
                                      :on-press #(re-frame/dispatch [::multiaccounts.key-storage/seed-phrase-next-pressed])
                                      :after    :main-icons/next}
                                     "Choose storage"]}]]))

(defn storage []
  [react/view
   [react/text :storage]])

(comment
  (-> re-frame.db/app-db deref keys)
  (-> re-frame.db/app-db deref
      :multiaccounts/login)

  )
