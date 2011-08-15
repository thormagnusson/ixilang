XiiLang(txt:true) // run this line w shift+return (then alt+right button)

dr1 -> |o   x   o   x   |
dr2 -> |  b   b   d   b |
dr3 -> | o iws  e jb  gi|

shake dr3
snapshot -> one
group drum -> dr1 dr2 dr3
nap drum 2

drum >> reverbS

scale minor
sos -> wood[1   5   3   2   ]
sob -> wood[1   5   3   2   ]+12
swap sob
future 2b:12 >> shake sob

pia ->  xylo[1 4 23  5  65 4 ]
( pia 142
future 4b:12 >> ( pia 142 1 1214
future 4b:12 >> ^ pia 15 123 19992
+ pia 12
future 2b:4 >> + pia 7 12 24 0

snapshot -> two
snapshot one
snapshot two

pia >> distort >> techno
scalepush iwato
instruments
bob -> bar[1   4   6   2   ]
/ bob 4
future 0.1:12 >> (( bob
future 0.1:12 >> )) bob
doze bob
perk bob
group woody -> sos sob
future 4:12 >> shake woody
snapshot -> three
doze pia
snapshot -> four
snapshot four

matrix 6

save demosession
kill
// now empty this document and run this line
load demosession
 
