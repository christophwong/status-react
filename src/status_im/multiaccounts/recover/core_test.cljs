(ns status-im.multiaccounts.recover.core-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [status-im.multiaccounts.recover.core :as models]
            [status-im.multiaccounts.create.core :as multiaccounts.create]
            [status-im.utils.security :as security]))

;;;; helpers


(deftest check-phrase-warnings
  (is (= :t/required-field (models/check-phrase-warnings ""))))

;;;; handlers

(deftest set-phrase
  (is (= {:db {:intro-wizard {:passphrase            "game buzz method pretty olympic fat quit display velvet unveil marine crater"
                              :passphrase-error      nil
                              :next-button-disabled? false}}}
         (models/set-phrase {:db {}} (security/mask-data "game buzz method pretty olympic fat quit display velvet unveil marine crater"))))
  (is (= {:db {:intro-wizard {:passphrase            "game buzz method pretty olympic fat quit display velvet unveil marine crater"
                              :passphrase-error      nil
                              :next-button-disabled? false}}}
         (models/set-phrase {:db {}} (security/mask-data "Game buzz method pretty Olympic fat quit DISPLAY velvet unveil marine crater"))))
  (is (= {:db {:intro-wizard {:passphrase            "game buzz method pretty zeus fat quit display velvet unveil marine crater"
                              :passphrase-error      nil
                              :next-button-disabled? false}}}
         (models/set-phrase {:db {}} (security/mask-data "game buzz method pretty zeus fat quit display velvet unveil marine crater"))))
  (is (= {:db {:intro-wizard {:passphrase            "   game\t  buzz method pretty olympic fat quit\t   display velvet unveil marine crater  "
                              :passphrase-error      nil
                              :next-button-disabled? false}}}
         (models/set-phrase {:db {}} (security/mask-data "   game\t  buzz method pretty olympic fat quit\t   display velvet unveil marine crater  "))))
  (is (= {:db {:intro-wizard {:passphrase            "game buzz method pretty 1234 fat quit display velvet unveil marine crater"
                              :passphrase-error      nil
                              :next-button-disabled? false}}}
         (models/set-phrase {:db {}} (security/mask-data "game buzz method pretty 1234 fat quit display velvet unveil marine crater")))))

(deftest store-multiaccount
  (let [new-cofx (models/store-multiaccount {:db {:intro-wizard
                                                  {:passphrase "game buzz method pretty zeus fat quit display velvet unveil marine crater"}}}
                                            (security/mask-data "thisisapaswoord"))]
    (is (::multiaccounts.create/store-multiaccount new-cofx))))

(deftest on-import-multiaccount-success
  (testing "importing a new multiaccount"
    (let [res (models/on-import-multiaccount-success
               {:db {:multiaccounts/multiaccounts {:acc1 {}}}}
               {:key-uid :acc2}
               nil)]
      (is (nil? (:utils/show-confirmation res)))))
  (testing "importing an existing multiaccount"
    (let [res (models/on-import-multiaccount-success
               {:db {:multiaccounts/multiaccounts {:acc1 {}}}}
               {:key-uid :acc1}
               nil)]
      (is (contains? res :utils/show-confirmation)))))

(deftest login-ma-keycard-pairing
  (testing "returns nil when no :multiaccounts/login"
    (let [res (models/login-ma-keycard-pairing
               {:multiaccounts/login nil
                :multiaccounts/multiaccounts
                {"0x1" {:keycard-pairing "keycard-pairing-code"}}}
               {})]
      (is (nil? res))))

  (testing "returns :keycard-pairing when :multiaccounts/login is present"
    (let [res (models/login-ma-keycard-pairing
               {:multiaccounts/login {:key-uid "0x1"}
                :multiaccounts/multiaccounts
                {"0x1" {:keycard-pairing "keycard-pairing-code"}}}
               {})]
      (is (= res "keycard-pairing-code")))))
