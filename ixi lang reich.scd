XiiLang.new(txt:true) // run this with SuperCollider eval (Shift + Return)

// run the following with ixi lang eval (alt + right arrow)

tonic 64
scale dorian
tempo 360

reich  ->  piano[1   2   5   6   7   2   1   6   5   2   7   6   ](1)
reichp ->  piano[1   2   5   6   7   2   1   6   5   2   7   6   ](1)

>shift reichp 1

future 6:48 >> >shift reichp 1


---- another version ----

tempo 180
reich  ->  piano[1 2 5 6 7 2 1 6 5 2 7 6 ]
reicho ->  piano[1 2 5 6 7 2 1 6 5 2 7 6 ]

future 8:12 >> >shift reicho 1

kill

