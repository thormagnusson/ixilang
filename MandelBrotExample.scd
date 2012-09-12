
// networked ixi lang (connecting ixi langs) demo file

// use examples from the BenoitLib
// download : https://github.com/cappelnord/BenoitLib

// Thanks Patrick and Benoit and Mandelbrot

m = MandelHub.start("thor");
m.gui;
m.metro;
// now run some code in a ixi lang document
m.changeTempo(130/60);
m.changeTempo(140/60, 30);
m.clear;

// this will sync with ixi lang
Pbind(
	\degree, Pseq([1, 4, 3, 2], inf), 
	\dur, Pseq(1!4, inf)
).play(quant:1)

Pbind(
	\degree, Pseq([1, 4, 3, 2]+7, inf), 
	\dur, Pseq(1!4, inf)
).play(quant:4)

