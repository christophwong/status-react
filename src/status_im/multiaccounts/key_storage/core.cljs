(ns status-im.multiaccounts.key-storage.core
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [status-im.ethereum.mnemonic :as mnemonic]
            [status-im.multiaccounts.core :as multiaccounts]
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

(fx/defn handle-seed-phrase-validation
  {:events [::seed-phrase-validated]}
  [{:keys [db] :as cofx} validation-error]
  (let [error? (-> validation-error
                   types/json->clj
                   :error
                   string/blank?
                   not)]
    (if error?
      (popover/show-popover cofx {:view :custom-seed-phrase})
      (fx/merge cofx
                (navigation/navigate-to-cofx :key-storage-stack {:screen :storage})))))

(fx/defn seed-phrase-next-pressed
  {:events [::seed-phrase-next-pressed]}
  [{:keys [db] :as cofx}]
  (let [{:keys [seed-phrase]} (:multiaccounts/key-storage db)]
    {::multiaccounts/validate-mnemonic [(mnemonic/sanitize-passphrase seed-phrase) #(re-frame/dispatch [::seed-phrase-validated %])]}))

(fx/defn keycard-storage-pressed
  {:events [::keycard-storage-pressed]}
  [{:keys [db]} selected?]
  {:db (assoc-in db [:multiaccounts/key-storage :keycard-storage-selected?] selected?)})

(re-frame/reg-fx
 ::validate-pub-key-derived-from-seed
 (fn [{:keys [seed-phrase public-key success-event error-event]}]
   (re-frame/dispatch
    (if (native-module/validate-pub-key-derived-from-seed seed-phrase public-key)
      success-event error-event))))


(fx/defn validate-seed-generated-public-key
  {:events [::validate-seed-generated-public-key]}
  [_ seed-phrase public-key]
  {::validate-pub-key-derived-from-seed {:seed-phrase seed-phrase
                                         :public-key public-key
                                         :success-event [::seed-integrity-verified]
                                         :error-event [::seed-invalid]}})


(comment
  (mnemonic/sanitize-passphrase "rocket rebel pasta kimchi kitty nani tokyo")
  (native-module/multiaccount-import-mnemonic "rocket mixed rebel affair umbrella legal resemble scene virus park deposit cargo" nil prn)
  )


