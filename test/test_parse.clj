(ns name.choi.joshua.fnparse.test-parse
  (:use clojure.contrib.test-is)
  (:require [name.choi.joshua.fnparse :as p]))

(deftest test-term
  (is (= ((p/term #(= % "true")) ["true" "THEN"])
         ["true" (list "THEN")])
      "created terminal rule works when first token fulfills validator")
  (is (nil? ((p/term #(= % "true")) ["false" "THEN"]))
      "created terminal rule fails when first token fails validator"))

(deftest test-lit
  (is (= ((p/lit "true") ["true" "THEN"])
         ["true" (list "THEN")])
      "created literal rule works when literal token present")
  (is (nil? ((p/lit "true") ["false" "THEN"]))
      "created literal rule fails when literal token not present"))

(deftest test-re-term
  (is (= ((p/re-term #"\s*true\s*") ["  true" "THEN"])
         ["  true" (list "THEN")])
      "created re-term rule works when first token matches regex")
  (is (nil? ((p/re-term #"\s*true\s*") ["false" "THEN"]))
      "created re-term rule fails when first token does not match regex"))

(deftest test-semantics
  (is (= ((p/semantics (p/lit "hi") #(str % \!)) ["hi" "THEN"])
         ["hi!" (list "THEN")])
      "created rule applies semantic hook to valid result of given rule")
  (is (nil? ((p/semantics (p/lit "hi") #(str % \!)) "RST"))
      "created rule fails when given subrule fails"))

(deftest test-constant-semantics
  (is (= ((p/constant-semantics (p/lit "hi") (hash-map :a 1)) ["hi" "THEN"])
         [{:a 1} (list "THEN")])
      "created rule returns constant value when given subrule does not fail"))

(deftest test-conc
  (let [identifier (p/semantics (p/term string?) symbol),
        equals-operator (p/semantics (p/lit "=") keyword),
        answer (p/lit "42"),
        truth (p/conc identifier equals-operator answer)]
    ; Parse the first symbols in the program "answer = 42 THEN"
    (is (= (truth ["answer" "=" "42" "THEN"])
           [['answer := "42"] (list "THEN")])
        "created concatenation rule works when valid symbols are present in order")
    ; Parse the first symbols in the program "answer = 42 THEN"
    (is (= (truth ["answer" "42" "=" "THEN"]) nil)
        "created concatenation rule fails when invalid symbols present")))

(deftest test-alt
  (let [literal-true (p/semantics (p/lit "true") (fn [_] true)),
        literal-false (p/semantics (p/lit "false") (fn [_] false)),
        literal-boolean (p/alt literal-true literal-false)]
    ; Parse the first symbol in the program "false THEN"
    (is (= (literal-boolean ["false" "THEN"])
           [false (list "THEN")])
        "created alternatives rule works with first valid rule product")
    ; Parse the first symbol in the program "aRSTIR"
    (is (nil? (literal-boolean ["aRSTIR"]))
        "created alternatives rule fails when no valid rule product present")))

(deftest test-opt
  (let [literal-true (p/semantics (p/lit "true") (fn [_] true))]
    ; Parse the first symbol in the program "true THEN"
    (is (= ((p/opt literal-true) ["true" "THEN"])
           [true (list "THEN")])
        "created option rule works when symbol present")
    ; Parse the first symbol in the program "THEN"
    (is (= ((p/opt literal-true) (list "THEN"))
           [nil (list "THEN")])
        "created option rule works when symbol absent")))

(deftest test-rep*
  (let [literal-true (p/semantics (p/lit "true") (fn [_] true))]
    ; Parse the first symbol in the program "true THEN"
    (is (= ((p/rep* literal-true) ["true" "THEN"])
           [[true] (list "THEN")])
        "created zero-or-more-repetition rule works when symbol present singularly")
    ; Parse the first symbol in the program "true true true THEN"
    (is (= ((p/rep* literal-true) ["true" "true" "true" "THEN"])
           [[true true true] (list "THEN")])
        "created zero-or-more-repetition rule works when symbol present multiply")
    ; Parse the first symbol in the program "THEN"
    (is (= ((p/rep* literal-true) (list "THEN"))
           [[] (list "THEN")])
     "created zero-or-more-repetition rule works when symbol absent"))
  (let [literal-char (p/term #(not= % \"))]
    ; Parse the first symbol in the program "THEN"
    (is (= ((p/rep* literal-char) (list \a \b \c))
           [[\a \b \c] nil])
        "created zero-or-more-repetition rule with a negative subrule works with no remainder")))

(deftest test-rep+
  (let [literal-true (p/semantics (p/lit "true") (fn [_] true))]
    ; Parse the first symbol in the program "true THEN"
    (is (= ((p/rep+ literal-true) ["true" "THEN"])
           [[true] (list "THEN")])
        "created one-or-more-repetition rule works when symbol present singularly")
    ; Parse the first symbol in the program "true true true THEN"
    (is (= ((p/rep+ literal-true) ["true" "true" "true" "THEN"])
           [[true true true] (list "THEN")])
        "created one-or-more-repetition rule works when symbol present multiply")
    ; Parse the first symbol in the program "THEN"
    (is (nil? ((p/rep+ literal-true) (list "THEN")))
        "created one-or-more-repetition rule fails when symbol absent")))

(deftest test-lit-seq
  ; Parse the first four symbols in the program "THEN"
  (is (= ((p/lit-seq "THEN") (seq "THEN print 42;"))
         [(vec "THEN") (seq " print 42;")])
      "created literal-sequence rule is based on sequence of given token sequencible"))

(deftest test-emptiness
  ; Parse the emptiness before the first symbol
  (is (= (p/emptiness (list "A" "B" "C"))
         [nil (list "A" "B" "C")])
      "emptyiness rule matches emptiness"))

(run-tests)
