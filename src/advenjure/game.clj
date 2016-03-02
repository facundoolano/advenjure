

;;;example game map

{:current-room :bedroom
 :room-map {}
 :inventory #{}}


;;; EXAMPLE ROOM DEFINITIONS

; this is garbage for now
; TODO use records instead

(def magazine {:names ["sports magazine" "magazine"]
               :description "The cover reads 'Sports Almanac 1950-2000'"
               :read "It's actually a naughty magazine once you remove the dust cover."
               :take true})


(def bedroom {:name "Bedroom"
              :intial-description "I woke up in a smelling little bedroom,
                                without windows or any furniture other than the bed I was laying in and a reading lamp."
              :description "A smelling bedroom. There was an unmade bed near the corner and a lamp by the bed."
              :items #{{:names ["bed"] :description "It was the bed I slept in."}
                       {:names ["reading lamp" "lamp"] :description "Nothing special about the lamp."}
                       magazine}
              :item-descriptions {"bed" "" ;empty means skip it while describing, already contained in room description
                                  "magazine" "Laying by the bed was a sports magazine."}}) ; use this when describing the room instead of "there's a X here"


(def drawer {:names ["drawer" "drawers" "chest" "chest drawer" "chest drawers" "drawer chest" "drawers chest"]
             :closed true ; if it has closed kw, means it responds to open/close.
             :items #{} ; if it has items set, means it can contain stuff.
             :description "A chest drawer"})


(def living {:name "Living Room"
             :intial-description "The living room was as smelly as the bedroom, and although there was a window,
                                it appeared to be nailed shut. There was a pretty good chance I'd choke to death
                                if I didn't leave the place soon."
             :description "A living room with a nailed shut window."})


;;; BUILD THE ACTUAL MAP
(def room-map (-> {}
                  (add-room :bedroom bedroom)
                  (add-room :living living)
                  (connect-rooms :bedroom :north :living)))
