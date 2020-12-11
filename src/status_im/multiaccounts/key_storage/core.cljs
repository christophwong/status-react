(ns status-im.multiaccounts.key-storage.core
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [status-im.ethereum.mnemonic :as mnemonic]
            [status-im.multiaccounts.core :as multiaccounts]
            [status-im.multiaccounts.recover.core :as multiaccounts.recover]
            [status-im.multiaccounts.model :as multiaccounts.model]
            [status-im.native-module.core :as native-module]
            [status-im.navigation :as navigation]
            [status-im.popover.core :as popover]
            [status-im.utils.fx :as fx]
            [status-im.utils.security :as security]
            [status-im.utils.types :as types]))

(fx/defn key-and-storage-management-pressed
  "This event can be dispatched before login and from profile and needs to redirect accordingly"
  {:events [::key-and-storage-management-pressed]}
  [cofx]
  (fx/merge cofx
            (navigation/navigate-to-cofx :key-storage-stack
                                         {:screen (if (multiaccounts.model/logged-in? cofx)
                                                    :actions-logged-in
                                                    :actions-not-logged-in)})))

(fx/defn move-keystore-checked
  {:events [::move-keystore-checked]}
  [{:keys [db] :as cofx} checked?]
  {:db (assoc-in db [:multiaccounts/key-storage :move-keystore-checked?] checked?)})

(fx/defn enter-seed-pressed
  "User is logged out and probably wants to move multiaccount to Keycard. Navigate to enter seed phrase screen"
  {:events [::enter-seed-pressed]}
  [cofx]
  (fx/merge cofx
            (navigation/navigate-to-cofx :key-storage-stack
                                         {:screen :seed-phrase})))

(fx/defn seed-phrase-input-changed
  {:events [::seed-phrase-input-changed]}
  [{:keys [db]} masked-seed-phrase]
  (let [seed-phrase (security/safe-unmask-data masked-seed-phrase)]
    {:db (update db :multiaccounts/key-storage assoc
                 :seed-phrase (string/lower-case seed-phrase)
                 :seed-shape-invalid? (or (empty? seed-phrase)
                                          (not (mnemonic/valid-length? seed-phrase)))
                 :seed-word-count (mnemonic/words-count seed-phrase))}))


(fx/defn key-uid-seed-mismatch
  {:events [::show-seed-key-uid-mismatch-error-popup]}
  [cofx _]
  (popover/show-popover cofx {:view :seed-key-uid-mismatch}))

(defn validate-seed-against-key-uid
  "Check if the key-uid was generated with the given seed-phrase"
  [{:keys [seed-phrase key-uid success-event error-event]}]
  (native-module/multiaccount-import-mnemonic
   seed-phrase nil
   (fn [result]
     (let [{:keys [keyUid]} (types/json->clj result)]
       ;; if the key-uid from app-db is same as the one returned by multiaccount import,
       ;; it means that this seed was used to generate this multiaccount
       (re-frame/dispatch (if (= key-uid keyUid)
                            success-event
                            error-event))))))

(re-frame/reg-fx
 ::validate-seed-against-key-uid
 validate-seed-against-key-uid)

(fx/defn seed-phrase-validated
  {:events [::seed-phrase-validated]}
  [{:keys [db] :as cofx} validation-error]
  (let [error? (-> validation-error
                   types/json->clj
                   :error
                   string/blank?
                   not)]
    (if error?
      (popover/show-popover cofx {:view :custom-seed-phrase})
      {::validate-seed-against-key-uid {:seed-phrase (-> db :multiaccounts/key-storage :seed-phrase)
                                        ;; Unique key-uid of the account for which we are going to move keys
                                        :key-uid (-> db :multiaccounts/login :key-uid)
                                        :success-event [:navigate-to :storage]
                                        :error-event [::show-seed-key-uid-mismatch-error-popup]}})))

(fx/defn choose-storage-pressed
  {:events [::choose-storage-pressed]}
  [{:keys [db] :as cofx}]
  (let [{:keys [seed-phrase]} (:multiaccounts/key-storage db)]
    {::multiaccounts/validate-mnemonic
     [(mnemonic/sanitize-passphrase seed-phrase)
      #(re-frame/dispatch [::seed-phrase-validated %])]}))

(fx/defn keycard-storage-pressed
  {:events [::keycard-storage-pressed]}
  [{:keys [db]} selected?]
  {:db (assoc-in db [:multiaccounts/key-storage :keycard-storage-selected?] selected?)})

(fx/defn warning-popup
  {:events [::show-transfer-warning-popup]}
  [cofx]
  (popover/show-popover cofx {:view :transfer-multiaccount-to-keycard-warning}))

(re-frame/reg-fx
 ::delete-multiaccount
 (fn [{:keys [key-uid on-success on-error]}]
   (native-module/delete-multiaccount
    key-uid
    (fn [result]
      (let [{:keys [error]} (types/json->clj result)]
        (if-not (string/blank? error)
          (on-error error)
          (on-success)))))))

(fx/defn delete-multiaccount-and-init-keycard-onboarding
  {:events [::delete-multiaccount-and-init-keycard-onboarding]}
  [{:keys [db] :as cofx}]
  (let [{:keys [key-uid]} (-> db :multiaccounts/login)]
    {::delete-multiaccount {:key-uid key-uid
                            :on-error #(re-frame/dispatch [::delete-multiaccount-error %])
                            :on-success #(re-frame/dispatch [::delete-multiaccount-success])}}))

#_"Multiaccount has been deleted from device. We now need to emulate the restore seed phrase process, and make the user land on Keycard setup screen.
To ensure that keycard setup works, we need to:
1. Import multiaccount, derive required keys and save them at the correct location in app-db
2. Take the user to :keycard-onboarding-intro screen in :intro-login-stack

The exact events dispatched for this flow if consumed from the UI are:
:m.r/enter-phrase-input-changed
:m.r/enter-phrase-next-pressed
:m.r/re-encrypt-pressed
:i/on-key-storage-selected ([:intro-wizard :selected-storage-type] is set to :advanced)
:m.r/select-storage-next-pressed

We don't need to take the exact steps, just set the required state and redirect to correct screen
"
(fx/defn handle-delete-multiaccount-success
  {:events [::delete-multiaccount-success]}
  [{:keys [db] :as cofx} _]
  (fx/merge cofx
            {::multiaccounts.recover/import-multiaccount {:passphrase (get-in db [:multiaccounts/key-storage :seed-phrase])
                                                          :password nil
                                                          :success-event ::import-multiaccount-success}}))

(fx/defn handle-multiaccount-import
  {:events [::import-multiaccount-success]}
  [{:keys [db] :as cofx} root-data derived-data]
  (fx/merge cofx
            {:db  (-> db
                      (update :intro-wizard
                              assoc :root-key root-data
                              :derived derived-data
                              :selected-storage-type :advanced)
                      (assoc-in [:keycard :flow] :recovery)
                      (dissoc :multiaccounts/key-storage))}
            (popover/hide-popover)
            (navigation/navigate-to-cofx :intro-stack {:screen :keycard-onboarding-intro})))

;; How to handle this?
(fx/defn handle-delete-multiaccount-error
  {:events [::delete-multiaccount-error]}
  [_ _]
  {})

(comment
  (mnemonic/sanitize-passphrase "rocket rebel pasta kimchi kitty nani tokyo")
  (native-module/multiaccount-import-mnemonic "rocket mixed rebel affair umbrella legal resemble scene virus park deposit cargo" nil
                                              (fn [result]
                                                (prn (types/json->clj result))))


  (native-module/delete-multiaccount "0x3831d0f22996a65970a214f0a94bfa9a63a21dac235d8dadb91be8e32e7d3ab7"
                                     (fn [result]
                                       (prn ::--rich-comment-res-> result)))
  )



