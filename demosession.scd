XiiLang(txt:true) // run this line w shift+return (then alt+right button)

dr1 -> |o   x   o   x   |
dr2 -> |  b   b   d   b |
dr3 -> | o iws  e jb  gi|

shake dr3
snapshot -> one
group drum -> dr1 dr2 dr3
nap drum 2

drum >> reverbS

doze drum
perk drum

oo ->        |a   b        k  d   |
oo ->  morph |a   b        k  d   |
oo -> morph@ |a   b        k  d   |
oo ->   fade |a   b        k  d   |
oo ->  fade@ |a   b        k  d   |
oo ->   wipe |a   b        k  d   |
oo ->  wipe@ |a   b        k  d   |
doze oo

scale minor
sos -> wood[1   5   3   2   ]
sob -> wood[1   5   3   2   ]+12
swap sob
future 2b:12 >> shake sob

pia ->  xylo[1 4 23  5  65 4 ]
( pia 142
future 4b:12 >> ( pia 1 2428 1214
future 4b:12 >> ^ pia 15 123 19992
+ pia 12
future 2b:4 >> + pia 7 12 24 0

a -> (135)
b -> $-7
chordag -> synth[1  a  1  b   35 ](818188)

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

tick  -> |t   t   t   t   |
tick2 -> |t  t  t  t  |
tick3 -> |tt tt   |
tock  -> |t t t t |*1.5

< tick 1928375

group woody -> sos sob
future 4:12 >> shake woody
snapshot -> three
doze pia
snapshot -> four
snapshot one
snapshot two
snapshot three
snapshot four

matrix 6
autocode 2 

savescore demoscore
store demosession

kill
bloo -> string[1 3 5   7 2 1   3   ]
blox -> string[1 3 5   7 2 17853   ]
scalepush iwato
scale blox minor
future 2b:5 >> scale bloo minor major harmonicMinor minor


// now empty this document and run this line
load demosession
// or this one
playscore demoscore
 
