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
   :portrait-id :ubyte
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
   :town #(when (not (unused-player? %1))
            (codec/cond-codec
             :ai-behavior :byte
             :town-set-allowed? codec/byte->bool
             :town (b/bits [:castle :rampart :tower :inferno :necropolis :dungeon :stronghold :fortress :conflux])
             :random? codec/byte->bool
             :has-main-town? codec/byte->bool
             :main-town (fn [town] (when (:has-main-town? town) main-town))))
   :hero #(when (not (unused-player? %1))
            (codec/cond-codec
             :random-hero? codec/byte->bool
             :main-hero-id :ubyte
             :main (fn [parsed-hero] (when (not= 255 (:main-hero-id parsed-hero)) hero))
             :unknown-byte :byte
             :heroes-count :ubyte
             :skip [:ubyte :ubyte :ubyte]
             :heroes (fn [parsed-hero] (b/repeated hero :length (:heroes-count parsed-hero)))))))

(def victory-loss-conditions
  (codec/cond-codec
   :win-type :ubyte
   :win #(when (not= 255 (:win-type %1))
           (codec/cond-codec
            :allow-normal? codec/byte->bool
            :applies-to-ai? codec/byte->bool
            :condition (case (int (:win-type %1))
                         0 [:byte :byte] ; ARTIFACT
                         1 [:byte :byte :int-le] ; GATHERTROOP
                         2 [:byte :int-le] ; GATHERRESOURCE
                         3 [:byte :byte :byte
                            :byte
                            :byte] ; BUILDCITY
                         4 [:byte :byte :byte] ; BUILDGRAIL
                         5 [:byte :byte :byte] ; BEATHERO
                         6 [:byte :byte :byte] ; CAPTURECITY
                         7 [:byte :byte :byte] ; BEATMONSTER
                         8 nil ; TAKEDWELLINGS
                         9 nil ; TAKEMINES
                         10 [:byte
                             :byte :byte :byte] ; TRANSPORTITEM
                         nil)))
   :loss-type :ubyte
   :loss #(when (not= 255 (:loss-type %1))
            (case (int (:loss-type %1))
              0 [:byte :byte :byte] ; LOSSCASTLE
              1 [:byte :byte :byte] ; LOSSHERO
              2 [:short-le] ; TIMEEXPIRES
              nil))))


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
   :unknown #(when (:message? %1) :int-le)))


(def resources (b/repeated :int-le :length 7))


(def quest
  (codec/cond-codec
   :mission-type :byte
   :quest #(when (:mission-type %1)
             (b/ordered-map
              :mission-data (case (int (:mission-type %1))
                              1 :int-le
                              2 :int-le
                              3 :int-le
                              4 :int-le
                              5 (b/repeated :short-le :prefix :byte)
                              6 (b/repeated :int-le :prefix :byte)
                              7 (b/repeated :int-le :length 7)
                              8 :byte
                              9 :byte
                              nil)
              :unknown-1 :int-le
              :first-visit-message codec/int-sized-string
              :next-visit-message codec/int-sized-string
              :completed-message codec/int-sized-string))))


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
   :artifacts? codec/byte->bool
   :artifacts #(when (:artifacts? %1) artifacts)
   :patrol-radius :ubyte
   :bio? codec/byte->bool
   :bio #(when (:bio? %1) codec/int-sized-string)
   :sex :byte
   :spells? codec/byte->bool
   :spells #(when (:spells? %1) (b/repeated :byte :length 9))
   :primary-skills? codec/byte->bool
   :primary-skills #(when (:primary-skills? %1) (b/repeated :byte :length 4))
   :unknown (b/repeated :byte :length 16)))


(def object-monster
  (codec/cond-codec
   :id :int-le
   :count :short-le
   :character :byte
   :msg? codec/byte->bool
   :msg #(when (:msg? %1)
           (b/ordered-map
            :text codec/int-sized-string
            :resources resources
            :art-id :short-le))
   :never-flees codec/byte->bool
   :never-grow codec/byte->bool
   :unknown-1 :byte
   :unknown-2 :byte))


(def object-message
  (b/ordered-map
   :message codec/int-sized-string
   :unknown :int-le))


(def reward
  (codec/cond-codec
   :type :byte
   :reward #(case (int (:type %1))
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
              10 [:short-le :short-le] ; CREATURE
              nil)
   :unknown :short-le))


(def object-seer-hut
  (codec/cond-codec
   :quest quest
   :reward #(if (> (get-in %1 [:quest :mission-type]) 0)
              reward
              [:byte :byte :byte])))


(def object-witch-hut
  (b/ordered-map
   :skill :int-le))


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
   :creatures (b/repeated :byte :length 14)
   :unknown-4 :int-le))


(def object-town
  (codec/cond-codec
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
                 (b/ordered-map
                  :unknown codec/byte->bool))
   :spells (b/repeated :byte :length 9)
   :unknown (b/repeated :byte :length 9)
   :events (b/repeated town-event :prefix :int-le)
   :unknown-4 (b/repeated :byte :length 1)
   :unknown-5 (b/repeated :byte :length 3)))


(def object-skip-int
  (b/ordered-map
   :unknown :int-le))


(def object-creature-generator
  (b/ordered-map
   :color :byte
   :unknown (b/repeated :byte :length 3)))


(def object-pandoras-box
  (codec/cond-codec
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
   :creatures (b/repeated creature :prefix :byte)
   :unknown (b/repeated :byte :length 8)))


(def object-random-dwelling
  (codec/cond-codec
   :owner :int-le
   :castle-index :int-le
   :allowed-town #(when (= 1 (:castle-index %1))
                    (b/bits [:castle :rampart :tower :inferno :necropolis :dungeon :stronghold :fortress :conflux]))
   :min :byte
   :max :byte))


(def object-random-dwelling-lvl
  (codec/cond-codec
   :owner :int-le
   :castle-index :int-le
   :allowed-town #(when (= 1 (:castle-index %1))
                    (b/bits [:castle :rampart :tower :inferno :necropolis :dungeon :stronghold :fortress :conflux]))))



(def object-random-dwelling-faction
  (b/ordered-map
   :owner :int-le
   :min :byte
   :max :byte))


(def object-quest-guard
  (codec/cond-codec
   :quest quest))


(def object-hero-placeholder
  (codec/cond-codec
   :unknown-1 :byte
   :hero-type-id :ubyte
   :unknown-2 #(when (= 255 (:hero-type-id %1)) :byte)))


(defn def->codec
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

    :mine object-skip-int
    :abandoned-mine object-skip-int
    :shrine-of-magic-incantation object-skip-int
    :shrine-of-magic-gesture object-skip-int
    :shrine-of-magic-thought object-skip-int
    :grail object-skip-int
    :shipyard object-skip-int
    :lighthouse object-skip-int

    :creature-generator1 object-creature-generator
    :creature-generator2 object-creature-generator
    :creature-generator3 object-creature-generator
    :creature-generator4 object-creature-generator

    :pandoras-box object-pandoras-box

    :random-dwelling object-random-dwelling
    :random-dwelling-lvl object-random-dwelling-lvl
    :random-dwelling-faction object-random-dwelling-faction

    :quest-guard object-quest-guard

    :hero-placeholder object-hero-placeholder
    nil))


(defn get-object-codec
  [defs-list]
  (codec/cond-codec
   :x :ubyte
   :y :ubyte
   :z :ubyte
   :def-index :int-le
   :placeholder (b/repeated :byte :length 5)
   :info #(def->codec (nth defs-list (:def-index %1)))))


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
   :teams-count :ubyte
   :teams #(when (not= 0 (:teams-count %1))
             (b/repeated :byte :length 8))
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
                                                 (if (:has-underground? %1) 2 1)))
   :defs (b/repeated def-info :prefix :int-le)
   :objects #(b/repeated (get-object-codec (:defs %1)) :prefix :int-le)))
