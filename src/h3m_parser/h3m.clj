(ns h3m-parser.h3m
  (:require [org.clojars.smee.binary.core :as b]
            [h3m-parser.codec :as codec]))


(def main-town
  (b/ordered-map
   :generate-hero-at-main-town codec/byte->bool
   :generate-hero codec/byte->bool
   :x :byte
   :y :byte
   :z :byte))


(def hero
  (codec/cond-codec
   :portrait-id :byte
   :name codec/int-sized-string))


(defn unused-player?
  [player]
  (and (not (:human-can-play? player))
       (not (:pc-can-play? player))))


(def player
  (codec/cond-codec
   :human-can-play? codec/byte->bool
   :pc-can-play? codec/byte->bool
   :skiped? #(when (unused-player? %1) (b/repeated :byte :length 13))
  ;  :behavior #(when (not (unused-player? %1)) :byte)
   :town #(when (not (unused-player? %1))
            (codec/cond-codec
             :behavior :byte
             :town-set-allowed? codec/byte->bool
             :town-bits (b/bits [:castle :rampart :tower :inferno :necropolis :dungeon :stronghold :fortress :conflux])
             :random? codec/byte->bool
             :has-main-town? codec/byte->bool
             :main-town (fn [town] (when (:has-main-town? town) main-town))))
   :hero #(when (not (unused-player? %1))
            (codec/cond-codec
             :random-hero? codec/byte->bool
             :hero-id :byte ; ubyte???
             ; TOOD
            ;  :main (fn [h] (when (not= 255 (:hero-id h))) hero)
             :unknown-byte :byte
             :heroes (b/repeated hero :prefix :int-le)))))


(def victory-loss-conditions
  (codec/cond-codec
   :victory :ubyte
   :ai-can-kill-all? #(when (not= 255 (:victory %1)) :short-le)
   :win-condition #(case (:victory %1)
                     0 [:byte :byte]
                     1 [:byte :byte :int-le]
                     2 [:byte :int-le]
                     3 [:byte :byte :byte
                        :byte
                        :byte]
                     4 [:byte :byte :byte]
                     5 [:byte :byte :byte]
                     6 [:byte :byte :byte]
                     7 [:byte :byte :byte]
                     8 nil
                     9 nil
                     10 [:byte
                         :byte :byte :byte]
                     nil)
   :loss :ubyte
   :loss-condition #(case (:loss %1)
                      1 [:byte :byte :byte]
                      2 [:byte :byte :byte]
                      3 [:byte :byte]
                      nil)))


(def disposed-hero
  (codec/cond-codec
   :id :byte
   :portrait :byte
   :name codec/int-sized-string
   :players :byte))


(def rumor
  (codec/cond-codec
   :name codec/int-sized-string
   :text codec/int-sized-string))


(def secondary-skill
  (b/ordered-map
   :skill :byte
   :value :byte))


(def artifact
  (codec/cond-codec
   :slots (b/repeated :short-le :length 16)
   :unknown-slot :short-le
   :spellbook :short-le
   :fifth-slot :short-le
   :bag (b/repeated :short-le :prefix :short-le)))


(def defined-hero
  (codec/cond-codec
   :expirience? codec/byte->bool
   :expirience #(when (:expirience? %1) :int-le)
   :secondary-skills? codec/byte->bool
   :secondary-skills #(when (:secondary-skills? %1) (b/repeated secondary-skill :prefix :int-le))
   :artifacts? codec/byte->bool
   :artifacts #(when (:artifacts? %1) artifact)
   :bio? codec/byte->bool
   :bio #(when (:bio? %1) codec/int-sized-string)
   :sex :byte
   :spells? codec/byte->bool
   :spells #(when (:spells? %1) (b/repeated :byte :length 9))
   :primary-skills? codec/byte->bool
   :primary-skills #(when (:primary-skills? %1) (b/repeated :byte :length 4))))


(def terrain-tile
  (b/ordered-map
   :terrain :ubyte
   :terrain-image-index :ubyte
   :river :ubyte
   :river-image-index :ubyte
   :road :ubyte
   :road-image-index :ubyte
   :mirror-config :ubyte))


(def def-info
  (codec/cond-codec
   :sprite-name codec/int-sized-string
   :passable-cells [:int-le :short-le]
   :active-cells [:int-le :short-le]
   :terrain-type :short-le
   :terrain-group :short-le
   :object-id codec/int->object
   :class-sub-id :int-le
   :group :ubyte
   :placement-order :ubyte
   :unknown (b/repeated :byte :length 16)))


(def creature
  (b/ordered-map
   :creature-id :short-le
   :count :short-le))


(def message-with-guard
  (codec/cond-codec
   :message? codec/byte->bool
   :message #(when (:message? %1) codec/int-sized-string)
   :guards? #(when (:message? %1) codec/byte->bool)
   :guards #(when (:guards? %1) (b/repeated creature :length 7))
   :unknown #(when (:message? %1) codec/byte->bool)))


(def resources (b/repeated :int-le :length 7))


(def object-event
  (codec/cond-codec
   :message message-with-guard
   :expirience :int-le
   :mana :int-le
   :morale :byte
   :luck :byte
   :resources resources
   :primary-skills :int-le
   :abillities (b/repeated :short-le :prefix :byte)
   :arts (b/repeated :short-le :prefix :byte)
   :spells (b/repeated :byte :prefix :byte)
   :creatures (b/repeated creature :prefix :byte)
   :unknown-1 (b/repeated :byte :length 8)
   :unknown-2 :byte
   :pc-can-activate? codec/byte->bool
   :one-time? codec/byte->bool
   :unknown-3 :int-le))


(def object-hero
  (codec/cond-codec
   ;; TODO
   ))


(defn get-codec-by-def-id
  [def-info]
  (case (:object-id def-info)
    26 object-event
    34 object-hero
    70 object-hero
    62 object-hero
    nil))


(defn get-object-codec
  [defs-list]
  (codec/cond-codec
   :x :ubyte
   :y :ubyte
   :z :ubyte
   :def-index :int-le
   :placeholder (b/repeated :byte :length 5)
   :info #(get-codec-by-def-id (nth defs-list (:def-index %1)))))


(def root
  (codec/cond-codec
   :map-version (b/constant :int-le 28)
   :has-players? codec/byte->bool
   :size :int-le
   :has-underground? codec/byte->bool
   :title codec/int-sized-string
   :description codec/int-sized-string
   :difficulty :byte
   :level-limit :byte
   :players (b/repeated player :length 8)
   :victory-loss-conditions victory-loss-conditions
   :teams-count :byte
   :teams (b/repeated :byte :length 8)
   :placeholder-1 (b/repeated :ubyte :length 20)
   :placeholder-2 (b/repeated :byte :prefix :int-le)
   :heroes (b/repeated disposed-hero :prefix :ubyte)
   :placeholder-3 (b/repeated :byte :length 31)
   :artifacts (b/repeated :byte :length 18)
   :spells (b/repeated :byte :length 9)
   :abillities (b/repeated :byte :length 4)
   :rumors (b/repeated rumor :prefix :int-le)
   :predefined-heroes (b/repeated (codec/cond-codec
                                   :defined? codec/byte->bool
                                   :hero #(when (:defined %1) defined-hero)) :length 156)
   :terrain #(b/repeated terrain-tile :length (* (* (:size %1) (:size %1))
                                                 (if (:has-underground? %1)
                                                   2
                                                   1)))
   :defs (b/repeated def-info :prefix :int-le)
  ;  :objects #(b/repeated (get-object-codec (:defs %1)) :prefix :int-le)
   ))
