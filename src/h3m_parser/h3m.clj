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


(def artifacts
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
   :artifacts #(when (:artifacts? %1) artifacts)
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
   :object codec/int->object
   :class-sub-id :int-le
   :group :ubyte
   :placement-order :ubyte
   :unknown (b/repeated :byte :length 16)))


(def creature
  (b/ordered-map
   :creature-id :short-le
   :count :short-le))


(def creature-set
  (b/repeated creature :length 7))


(def message-with-guard
  (codec/cond-codec
   :message? codec/byte->bool
   :message #(when (:message? %1) codec/int-sized-string)
   :guards? #(when (:message? %1) codec/byte->bool)
   :guards #(when (:guards? %1) creature-set)
   :unknown #(when (:message? %1) codec/byte->bool)))


(def resources (b/repeated :int-le :length 7))


(defn quest [mission-type]
  (case mission-type
    2 nil))


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
   :id :int-le
   :owner :byte
   :sub-id :byte
   :name? codec/byte->bool
   :name #(when (:name? %1) codec/int-sized-string)
   :expirience? codec/byte->bool
   :expirience #(when (:expirience? %1) :int-le)
   :portrait? codec/byte->bool
   :portrait #(when (:portrait? %1) :byte)
   :secondary-skills? codec/byte->bool
   :secondary-skills #(when (:secondary-skills? %1) (b/repeated secondary-skill :prefix :int-le))
   :garrison? codec/byte->bool
   :garrison #(when (:garrison? %1) creature-set)
   :formation :byte
   :artifacts artifacts
   :patrol-radius :byte
   :bio? codec/byte->bool
   :bio #(when (:bio? %1) codec/int-sized-string)
   :sex :byte
   :spells (b/repeated :byte :length 9)
   :primary-skills? codec/byte->bool
   :primary-skills #(when (:primary-skills? %1) (b/repeated :byte :length 4))
   :unknown (b/repeated :byte :length 16)))


;; TODO save random-monster level
(def object-monster
  (codec/cond-codec
   :unknown :int-le
   :count :short-le
   :character :byte
   :msg? codec/byte->bool
   :msg #(when (:msg? %1)
           (b/ordered-map
            :text codec/int-sized-string
            :resources resources
            :unknown :short-le))
   :never-flees codec/byte->bool
   :never-grow codec/byte->bool
   :unknown :short-le))


(def object-message
  (b/ordered-map
   :message codec/int-sized-string
   :unknown :int-le))


(def reward
  (codec/cond-codec
   :type :byte
   :reward #(case (:type %1)
             ; 0 NOTHING
              1 :int-le ; EXPERIENCE
              2 :int-le ; MANA_POINTS
              3 :byte ; MORALE_BONUS
              4 :byte ; LUCK_BONUS
              5 [:byte :int-le] ; RESOURCES
              6 [:byte :byte] ; PRIMARY_SKILL
              7 [:byte :byte] ; SECONDARY_SKILL
              8 [:short-le] ; ARTIFACT
              9 :byte ; SPELL
              10 :int-le ; CREATURE
              nil)))


(def object-seer-hut
  (codec/cond-codec
   :mission-type :byte
   :quest #(quest (:mission-type %1))
   :reward #(if (> 0 (:mission-type %1))
              reward
              [:byte :byte :byte])))


(def object-witch-hut :int-le)


(def object-scholar
  (b/ordered-map
   :unknown-1 (b/repeated :byte :length 2)
   :unknown-2 (b/repeated :byte :length 6)))


(def object-garrison
  (b/ordered-map
   :unknown-1 :int-le
   :creatures creature-set
   :unknown-2 codec/byte->bool
   :unknown-3 (b/repeated :byte :length 8)))


(def object-artifact
  (b/ordered-map
   :message message-with-guard))


(def object-spell
  (b/ordered-map
   :message message-with-guard
   :unknown :int-le))


(def object-resource
  (b/ordered-map
   :message message-with-guard
   :unknown-1 :int-le
   :unknown-2 :int-le))


(def town-event
  (b/ordered-map
   :msg-1 codec/int-sized-string
   :msg-2 codec/int-sized-string
   :resources resources
   :unknown-1 :byte
   :unknown-2 :byte
   :computer-affected :byte
   :first-occurence :short-le
   :next-occurence :byte
   :unknown-3 (b/repeated :byte :length 17)
   :building (b/repeated :byte :length 6)
   :creatures (b/repeated :byte :length 14)))


(def object-town
  (b/ordered-map
   :id :int-le
   :owner :byte
   :name? codec/byte->bool
   :name #(when (:name? %1) codec/int-sized-string)
   :garrison? codec/byte->bool
   :garrison #(when (:garrison? %1) creature-set)
   :formation codec/byte->bool
   :buildings? codec/byte->bool
   :buildings #(if (:buildings? %1)
                 (b/ordered-map
                  :built (b/repeated :byte :length 6)
                  :fobidden (b/repeated :byte :length 6))
                 :unknown codec/byte->bool)
   :spells (b/repeated :byte :length 9)
   :unknown (b/repeated :byte :length 9)
   :events (b/repeated town-event :prefix :int-le)
   :unknown-4 (b/repeated :byte :length 4)
   :unknown-5 (b/repeated :byte :length 3)))


(def object-mine :int-le)


(def object-creature-generator
  (b/ordered-map
   :color :byte
   :unknown (b/repeated :byte :length 3)))


(def object-pandoras-box
  :message message-with-guard
  :expirience :int-le
  :mana :int-le
  :morale :byte
  :luck :byte
  :resources resources
  :primary-skills (b/repeated :byte :length 4)
  :abillities (b/repeated :short-le :prefix :byte)
  :arts (b/repeated :short-le :prefix :byte)
  :spells (b/repeated :byte :prefix :byte)
  :creatures (b/repeated :byte :prefix :byte)
  :unknown (b/repeated :byte :length 8))


(def object-random-dwelling
  (b/ordered-map
   :unknown :int-le
   :castle-index :int-le
   :allowed-town #(when (= 1 (:castle-index %1)) :short-le) ; TODO bits
   :min :byte
   :max :byte))


(def object-random-dwelling-lvl
  (b/ordered-map
   :unknown :int-le
   :castle-index :int-le
   :allowed-town #(when (= 1 (:castle-index %1)) :short-le) ; TODO bits
   ))


(def object-random-dwelling-faction
  (b/ordered-map
   :unknown :int-le
   :min :byte
   :max :byte))


(def object-quest-guard
  (b/ordered-map
   :mission-type :byte
   :quest #(quest (:mission-type %1))))


(def object-shipyard :int-le)


(def object-hero-placeholder
  (codec/cond-codec
   :unknown-1 :byte
   :hero-type-id :ubyte
   :unknown-2 #(when (= 255 (:hero-type-id %1)) :byte)))


(def object-lighthous :int-le)


(defn get-codec-by-def-id
  [def-info]
  (case (:object def-info)
    :event object-event

    :hero object-hero
    :random-hero object-hero
    :prison object-hero

    :monster object-monster
    :random-monster object-monster
    :random-monster-l1 object-monster
    :random-monster-l2 object-monster
    :random-monster-l3 object-monster
    :random-monster-l4 object-monster
    :random-monster-l5 object-monster
    :random-monster-l6 object-monster
    :random-monster-l7 object-monster

    :ocean-bottle object-message
    :sign object-message

    :seer-hut object-seer-hut

    :witch-hut object-witch-hut

    :scholar object-scholar

    :garrison object-garrison
    :garrison2 object-garrison

    :artifact object-artifact
    :random-art object-artifact
    :random-treasure-art object-artifact
    :random-minor-art object-artifact
    :random-major-art object-artifact
    :random-relic-art object-artifact

    :spell-scroll object-spell

    :random-resource object-resource
    :resource object-resource

    :random-town object-town
    :town object-town

    :mine object-mine
    :abandoned-mine object-mine
    :shrine-of-magic-incantation object-mine
    :shrine-of-magic-gesture object-mine
    :shrine-of-magic-thought object-mine
    :grail object-mine

    :creature-generator1 object-creature-generator
    :creature-generator2 object-creature-generator
    :creature-generator3 object-creature-generator
    :creature-generator4 object-creature-generator

    :pandoras-box object-pandoras-box

    :random-dwelling object-random-dwelling
    :random-dwelling-lvl object-random-dwelling-lvl
    :random-dwelling-faction object-random-dwelling-faction

    :quest-guard object-quest-guard

    :shipyard object-shipyard

    :hero-placeholder object-hero-placeholder

    :lighthouse object-lighthous
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
   :predefined-heroes (b/repeated (b/repeated defined-hero :prefix :ubyte) :length 156)
   :terrain #(b/repeated terrain-tile :length (* (* (:size %1) (:size %1))
                                                 (if (:has-underground? %1)
                                                   2
                                                   1)))
   :defs (b/repeated def-info :prefix :int-le)
   :objects #(b/repeated (get-object-codec (:defs %1)) :prefix :int-le)))
