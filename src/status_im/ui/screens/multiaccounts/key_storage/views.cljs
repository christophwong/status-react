(ns status-im.ui.screens.multiaccounts.key-storage.views
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require [quo.core :as quo]
            [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.multiaccounts.core :as multiaccounts]
            [status-im.multiaccounts.key-storage.core :as multiaccounts.key-storage]
            [status-im.react-native.resources :as resources]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.chat-icon.screen :as chat-icon.screen]
            [status-im.ui.components.topbar :as topbar]
            [status-im.ui.components.toolbar :as toolbar]
            [status-im.ui.components.accordion :as accordion]
            [status-im.ui.screens.multiaccounts.views :as multiaccounts.views]
            [status-im.ui.screens.multiaccounts.key-storage.styles :as styles]
            [status-im.utils.config :as config]
            [status-im.utils.platform :as platform]))

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

(defn keycard-subtitle []
  [react/view
   [react/text {:style {:color colors/gray}} "Requires an empty Keycard"]
   [react/view {:flex-direction :row
                :align-items    :center}
    [react/text {:style               {:color colors/blue}
                 :accessibility-label :learn-more
                 :on-press #(js/alert :press)}
     (i18n/label :learn-more)]
    [vector-icons/icon :main-icons/tiny-external {:color  colors/blue
                                                  :width  16
                                                  :height 16}]]])

(defn keycard-upsell-banner []
  [react/view {:background-color "#2C5955" ;; TODO(shivekkhurana): This should be themed, #DDF8F4 for light mode
               :border-radius 16
               :margin 16
               :padding-horizontal 12
               :padding-vertical 8
               :flex-direction :row}
   [react/view
    [react/image {:source (resources/get-theme-image :keycard)
                  :resize-mode :contain
                  :style {:width 48
                          :height 48}}]]
   [react/view {:flex 1
                :margin-left 12}
    [react/text {:style {:font-size 20
                         :font-weight "700"}}
     "Get a Keycard"]
    [react/text
     "Your portable, easy to use hardware wallet"]]])

(defview storage []
  (letsubs
   [{:keys [keycard-storage-selected?]} [:multiaccounts/key-storage]]
   [react/view {:flex 1}
    [local-topbar "Choose storage"]
    [react/view {:style styles/help-text-container}
     [react/text {:style styles/help-text}
      "Choose a new location to save your keystore file"]]
    [react/view
     [quo/list-header "Current"]
     [quo/list-item {:title     "This device"
                     :text-size :base
                     :icon      :main-icons/mobile
                     :disabled  true}]
     [quo/list-header "New"]
     [quo/list-item {:title              "Keycard"
                     :subtitle           "Requires an empty Keycard"
                     :subtitle-max-lines 4
                     :icon               :main-icons/keycard
                     :active             keycard-storage-selected?
                     :on-press           #(re-frame/dispatch [::multiaccounts.key-storage/keycard-storage-pressed (not keycard-storage-selected?)])
                     :accessory          :radio}]]
    [react/view {:flex            1
                 :justify-content :flex-end}
     [keycard-upsell-banner]
     [toolbar/toolbar {:show-border? true
                       :right        [quo/button
                                      {:type     :secondary
                                       :disabled (not keycard-storage-selected?)
                                       :on-press #(re-frame/dispatch [::multiaccounts.key-storage/seed-phrase-next-pressed])}
                                      "Confirm"]}]]]))

(comment
  (-> re-frame.db/app-db deref keys)
  (-> re-frame.db/app-db deref
      :multiaccounts/login)

  (-> re-frame.db/app-db
      deref
      :multiaccounts/key-storage)


  ;; UI flow
  (do
    ;; Goto key management actions screen
    (re-frame/dispatch [::multiaccounts.key-storage/key-and-storage-management-pressed])

    ;; Check move key store checkbox
    (re-frame/dispatch [::multiaccounts.key-storage/move-keystore-checked true])

    ;; Goto enter seed screen
    (re-frame/dispatch [::multiaccounts.key-storage/enter-seed-pressed])

    ;; Enter seed phrase
    ;; status-im.utils.security is not explictly required because I know that it will be loaded by multiaccounts.views ns, hacky but works

    ;; invalid seed
    ;; (re-frame/dispatch [::multiaccounts.key-storage/seed-phrase-input-changed (status-im.utils.security/mask-data "h h h h h h h h h h h h")])

    ;; valid seed for Dim Venerated Yaffle (this is just a test account, okay to leak seed)
    (re-frame/dispatch [::multiaccounts.key-storage/seed-phrase-input-changed
                        (status-im.utils.security/mask-data "rocket mixed rebel affair umbrella legal resemble scene virus park deposit cargo")])

    ;; Click choose storage
    (re-frame/dispatch [::multiaccounts.key-storage/seed-phrase-next-pressed])
    )
  )
