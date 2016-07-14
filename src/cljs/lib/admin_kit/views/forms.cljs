(ns admin-kit.views.forms
  (:require [reagent.core :as reagent]
            [admin-kit.transit :as transit]
            [clojure.string :as str]))

(defmulti ^:export render-field
  (fn [field value rendered updater] (:type field)))

(defmethod render-field :default [field value rendered _]
  [:p.form-control-static {:label (:label field)} rendered])

(defmethod render-field :text [field value _ updater]
  (updater (str value))
  [:input.form-control
   {:type :text
    :placeholder (:label field)
    :default-value value
    :on-change (fn [e] (updater (.. e -target -value)))}])

(defmethod render-field :password [field value _ updater]
  [:input.form-control
   {:type :password
    :placeholder (:label field)
    :default-value value
    :on-change (fn [e] (updater (.. e -target -value)))}])

(defmethod render-field :select [field value _ updater]
  (let [values (:values field)
        value (str (or value (first (keys values))))]
    (updater value)
    [:select.form-control
     {:type :select
      :default-value value
      :on-change (fn [e]
                   (let [target (.-target e)]
                     (updater (-> (.-options target)
                                  (aget (.-selectedIndex target))
                                  .-value))))}
     (for [[val label] values]
       ^{:key val} [:option {:value val} label])]))

(defmethod render-field :radio [field value _ updater]
  (let [values (:values field)
        value (str (or value (first (keys values))))
        radio (fn [val]
                [:input {:type :radio
                         :value val
                         :checked (= val value)
                         :on-change #(updater (.. % -target -value))}])
        aligned (fn [[val label]]
                  ^{:key val} [:div.radio [:label [radio val] label]])
        inlined (fn [[val label]]
                  ^{:key val} [:label.radio-inline [radio val] label])]
    (updater value)
    [:div (map (if (:aligned? field) aligned inlined)
               (:values field))]))

(defmethod render-field :checkbox [field value _ updater]
  (let [value (boolean value)]
    (updater value)
    [:label.checkbox-inline
     [:input {:type :checkbox
              :checked value
              :on-change (fn [e] (updater (.. e -target -checked)))}]
     (get (into {} (:values field)) true)]))

(defmethod render-field :multi-select [field value _ updater]
  (let [value (or value [])]
    (updater value)
    [:select.form-control
     {:type :select
      :multiple true
      :value (into-array value)
      :on-change (fn [e]
                   (let [target (.-target e)]
                     (->> (array-seq (.-selectedOptions target))
                          (mapv #(.-value %))
                          updater)))}
     (for [[val label] (:values field)]
       ^{:key val} [:option {:value val} label])]))

(defmethod render-field :multi-checkbox [field value _ updater]
  (let [value (or value #{})
        checkbox (fn [val]
                   [:input {:type :checkbox
                            :checked (contains? value val)
                            :on-change (fn [e]
                                         (if (.. e -target -checked)
                                           (updater (conj value val))
                                           (updater (disj value val))))}])
        aligned (fn [[val label]]
                  ^{:key val} [:div.checkbox [:label [checkbox val] label]])
        inlined (fn [[val label]]
                  ^{:key val} [:label.checkbox-inline [checkbox val] label])]
    (updater value)
    [:div (map (if (:aligned? field) aligned inlined)
               (:values field))]))

(defmethod render-field :file [field value _ updater]
  (letfn [(on-change [e]
            (let [file (aget (.. e -target -files) 0)
                  reader (js/FileReader.)]
              (.addEventListener reader "load"
                (fn [] (-> (.-result reader)
                           (str/replace #"^data:[^,]+?," "")
                           ((fn [base64] (transit/file (.-name file) base64)))
                           updater))
                false)
              (.readAsDataURL reader file)))]
    [:input {:type :file :on-change on-change}]))
