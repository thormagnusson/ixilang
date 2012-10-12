
XiiLangInstr {
	
	classvar instrDict;
	var project;
	var sampleNames, samplePaths, nrOfSampleSynthDefs;
	var defaultsynthdesclib, synthdesclib;
	var bufferPool, bufferDict;
	var numChan;
	
	*new {| project, loadsamples=true, numChannels |
		^super.new.initXiiLangInstr(project, loadsamples, numChannels);
		}
		
	initXiiLangInstr {| argproject, loadsamples, numChannels |
		project = argproject;
		defaultsynthdesclib = SynthDescLib(\xiilang);
		bufferPool = []; // here in order to free buffers when doc is closed
		bufferDict = (); // used for morphing synths
		numChan = numChannels;
		// ----------------------------------------------------------------------------------
		// --------------------------- unique project synthdefs  ----------------------------
		// ----------------------------------------------------------------------------------

		synthdesclib = SynthDescLib(project.asSymbol);
		("ixilang/"++project++"/synthdefs.scd").load; // instead of loadPath
		
		// thisProcess.interpreter.executeFile("ixilang/"++project++"/synthdefs.scd");
		// was - > ("ixilang/"++project++"/synthdefs.scd").loadPath;


	
		// ----------------------------------------------------------------------------------
		// ---------------------------- percussive instruments  -----------------------------
		// ----------------------------------------------------------------------------------
		
		// ---------------------- sample based instruments -----------------------------
		if(loadsamples, {		
			samplePaths = ("ixilang/"++project++"/samples/*").pathMatch;
			//samplePaths = samplePaths.reject({ |path| path.basename.splitext[1] == "scd" }); // not including the keymapping files
			//samplePaths = samplePaths.reject({ |path| path.basename.splitext[1] == "ixi" }); // not including the keymapping files
			sampleNames = samplePaths.collect({ |path| path.basename.splitext[0]});
			
			[\samplenames, sampleNames].postln;
			if(samplePaths == [], {
				"-------------------------- NOTE ---------------------------".postln;
				"ixi lang : No samples were found to map to the keys. You need to put samples into the 'samples' folder of your project. (Default project is called 'default', but create your own project by creating a new folder next to the 'default'folder. \nSee the XiiLang.html help file".postln;
				"------------------------------------------------------------".postln;
		
			}, {
			
			// there might be more samples in the folder than the 52 keys of the keyboard, so load them as well
			nrOfSampleSynthDefs = if(sampleNames.size < 52, {52}, {sampleNames.size});
			
			/*
			samplePaths.do({arg filepath, i;
				var file, chnum;
				file = SoundFile.new;
				file.openRead(filepath);
				chnum = file.numChannels;
				file.close;
				SynthDef(sampleNames[i].asSymbol, {arg out=0, freq=440, amp=0.3, pan=0;
						var buffer, player, env, signal;
						buffer = Buffer.read(Server.default, filepath);
						player= if(chnum==1, { 
							PlayBuf.ar(1, buffer, (freq.cpsmidi-60).midiratio)!2 }, {
							PlayBuf.ar(2, buffer, (freq.cpsmidi-60).midiratio)});
						env = EnvGen.ar(Env.perc(0.01, 0.4), doneAction:2);
						signal = player * env * amp;
						Out.ar(out, signal);
				}).add;
			});
			*/
			
			/*
			samplePaths.do({arg filepath, i;
				var file, chnum;
				file = SoundFile.new;
				file.openRead(filepath);
				chnum = file.numChannels;
				file.close;
				SynthDef(sampleNames[i].asSymbol, {arg out=0, freq=440, amp=0.3, pan=0, noteamp=1, dur;
						var buffer, player, env, signal;
						buffer = Buffer.read(Server.default, filepath);
						player= Select.ar(noteamp,
							[ // playMode 2 - the sample player mode
							if(chnum==1, { 
							LoopBuf.ar(1, buffer, (freq.cpsmidi-60).midiratio, 1, 0, 0, 44100*60*10)!2 }, {
							LoopBuf.ar(2, buffer, (freq.cpsmidi-60).midiratio, 1, 0, 0, 44100*60*10)})
							* EnvGen.ar(Env.linen(0.0001, 60*60, 0.0001))
							, // playMode 1 - the rhythmic mode
							if(chnum==1, { 
							PlayBuf.ar(1, buffer, (freq.cpsmidi-60).midiratio)!2 }, {
							PlayBuf.ar(2, buffer, (freq.cpsmidi-60).midiratio)})
							* EnvGen.ar(Env.perc(0.01, 0.4))
							]);
						// I use DetectSilence rather than doneAction in Env.perc, as a doneAction in Env.perc
						// would also be running (in Select) thus killing the synth even in {} mode
						// I therefore add 0.02 so the 
						DetectSilence.ar(player, 0.001, 0.1, 2);
						//signal = player * amp * Lag.kr(noteamp, dur); // works better without lag
						signal = player * amp * noteamp;
						Out.ar(out, signal);
				}).add;
			});
			*/
			
			// 52 is the number of keys (lower and uppercase letters) supported
			nrOfSampleSynthDefs.do({arg i;
				var file, chnum, filepath;
				filepath = samplePaths.wrapAt(i);
				file = SoundFile.new;
				file.openRead(filepath);
				chnum = file.numChannels;
				file.close;
				SynthDef(sampleNames.wrapAt(i).asSymbol, {arg out=0, freq=261.63, amp=0.3, pan=0, noteamp=1, sustain=0.4;
						var buffer, player, env, signal, killer;
						bufferPool = bufferPool.add(buffer = Buffer.read(Server.default, filepath));
						// morpher instruments won't be stereo, so I just read one channel
						//bufferDict[sampleNames.wrapAt(i).asSymbol] = Buffer.readChannel(Server.default, filepath, channels: [0]);
						player = Select.ar(noteamp,
							[ // playMode 2 - the sample player mode
							if(chnum==1, { 
								LoopBuf.ar(1, buffer, (freq.cpsmidi-60).midiratio, 1, 0, 0, 44100*60*10) 
							}, {
								LoopBuf.ar(2, buffer, (freq.cpsmidi-60).midiratio, 1, 0, 0, 44100*60*10).sum
							})
								* EnvGen.ar(Env.linen(0.0001, 60*60, 0.0001))
							, // playMode 1 - the rhythmic mode
							if(chnum==1, { 
								PlayBuf.ar(1, buffer, (freq.cpsmidi-60).midiratio) 
							}, {
								PlayBuf.ar(2, buffer, (freq.cpsmidi-60).midiratio).sum
							})
								* EnvGen.ar(Env.perc(0.01, sustain))
							]);
						
						// I use DetectSilence rather than doneAction in Env.perc, as a doneAction in Env.perc
						// would also be running (in Select) thus killing the synth even in {} mode
						// I therefore add 0.02 so the 
						DetectSilence.ar(player, 0.001, 0.5, 2);
						//signal = player * amp * Lag.kr(noteamp, dur); // works better without lag
						signal = player * amp * noteamp;
						Out.ar(out, PanAz.ar(numChan, signal, pan, orientation: 0));
				}).add;
			});
	
			});
		});
		/*
		 Synth(\machine)
		*/
		
			nrOfSampleSynthDefs.do({arg i;
				var filepath;
				filepath = samplePaths.wrapAt(i);
				bufferDict[sampleNames.wrapAt(i).asSymbol] = Buffer.readChannel(Server.default, filepath, channels: [0]);
			});
		
		// explore hop size and loop in PlayBuf
		
		/*
		SynthDef(\morph, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.05, morphtime, 0.02]), t_trig, doneAction:2);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.05, morphtime, 0.02]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			chain = PV_Morph(chainA, chainB, EnvGen.ar(Env.new([0, 1], [morphtime]), t_trig) ); 
			Out.ar(out,  Pan2.ar(IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime]), t_trig)) * amp);
		}).add;
		
		SynthDef(\fade, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.05, morphtime, 0.02]), t_trig, doneAction:2);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.05, morphtime, 0.02]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			//chain = PV_Morph(chainA, chainB, SinOsc.ar(dur.reciprocal).range(0, 1) ); 
			chain = PV_XFade(chainA, chainB, EnvGen.ar(Env.new([0, 1], [morphtime]), t_trig) ); 
			Out.ar(out,  Pan2.ar(IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime]), t_trig)) * amp);
		}).add;
		
		SynthDef(\wipe, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.05, morphtime, 0.02]), t_trig, doneAction:2);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.05, morphtime, 0.02]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			//chain = PV_Morph(chainA, chainB, SinOsc.ar(dur.reciprocal).range(0, 1) ); 
			chain = PV_SoftWipe(chainA, chainB, EnvGen.ar(Env.new([0, 1], [morphtime]), t_trig) ); 
			Out.ar(out,  Pan2.ar(IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime]), t_trig)) * amp);
		}).add;
*/

/*
// old synthdef 
		SynthDef(\morph, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.05, dur, 0.02]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.05, dur, 0.02]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			//chain = PV_Morph(chainA, chainB, SinOsc.ar(dur.reciprocal).range(0, 1) ); 
			chain = PV_Morph(chainA, chainB, EnvGen.ar(Env.new([0, 1], [dur]), t_trig) ); 
			Out.ar(out,  Pan2.ar(IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [dur]), t_trig,)) * amp);
		}).add;

*/

		SynthDef(\morph, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			chain = PV_Morph(chainA, chainB, EnvGen.ar(Env.new([0, 1], [morphtime+0.1])) ); 
			Out.ar(out,  PanAz.ar(numChan, IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.3]), t_trig, doneAction:2), orientation: 0) * amp);
		}).add;

		SynthDef(\fade, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			chain = PV_XFade(chainA, chainB, EnvGen.ar(Env.new([0, 1], [morphtime+0.1])) ); 
			Out.ar(out,  PanAz.ar(numChan, IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.3]), t_trig, doneAction:2), orientation: 0.0) * amp);
		}).add;


		SynthDef(\wipe, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			chain = PV_SoftWipe(chainA, chainB, EnvGen.ar(Env.new([-0.95, 0.95], [morphtime+0.1])) ); 
			Out.ar(out,  PanAz.ar(numChan, IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.3]), t_trig, doneAction:2), orientation: 0.0) * amp);
		}).add;

		SynthDef(\minus, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			chain = PV_MagMinus(chainA, chainB, EnvGen.ar(Env.new([0, 1], [morphtime+0.1])) ); 
			Out.ar(out,  PanAz.ar(numChan, IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.3]), t_trig, doneAction:2), orientation: 0.0) * amp);
		}).add;

		SynthDef(\common, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			chain = PV_CommonMag(chainA, chainB, EnvGen.ar(Env.new([0, 1], [morphtime+0.1])) ); 
		//	chain = PV_CommonMag(chainA, chainB, 0.1, 0.1); 
			Out.ar(out,  PanAz.ar(numChan, IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.3]), t_trig, doneAction:2), orientation: 0.0) * amp);
		}).add;

		SynthDef(\binwipe, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			chain = PV_BinWipe(chainA, chainB, EnvGen.ar(Env.new([0, 1], [morphtime+0.1])) ); 
			Out.ar(out,  PanAz.ar(numChan, IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.3]), t_trig, doneAction:2), orientation: 0.0) * amp);
		}).add;

		SynthDef(\copy, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			chain = PV_CopyPhase(chainA, chainB, EnvGen.ar(Env.new([0, 1], [morphtime+0.1])) ); 
			Out.ar(out,  PanAz.ar(numChan, IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.3]), t_trig, doneAction:2), orientation: 0.0) * amp);
		}).add;

		SynthDef(\mul, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			chain = PV_MagMul(chainA, chainB ); 
			Out.ar(out,  PanAz.ar(numChan, IFFT(chain)*0.25, EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.3]), t_trig, doneAction:2), orientation: 0.0) * amp);
		}).add;

		SynthDef(\smear, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			chain = PV_MagSmear(chainA, EnvGen.ar(Env.new([0, 1], [morphtime+0.1]))*20 ); 
			Out.ar(out,  PanAz.ar(numChan, IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.3]), t_trig, doneAction:2), orientation: 0.0) * amp);
		}).add;

		SynthDef(\subtract, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			chain = PV_MagSubtract(chainA, chainB, -10 ); 
			Out.ar(out,  PanAz.ar(numChan, IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.3]), t_trig, doneAction:2), orientation: 0.0) * amp);
		}).add;

		SynthDef(\rand, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			chain = PV_RandWipe(chainA, chainB, EnvGen.ar(Env.new([0, 1], [morphtime]), t_trig) ); 
			Out.ar(out,  PanAz.ar(numChan, IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.3]), t_trig, doneAction:2), orientation: 0.0) * amp);
		}).add;
		
		SynthDef(\comb, { arg out=0, freq=261.63, panFrom=0, panTo=0, amp=0.3, buf1, buf2, dur, morphtime=1, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.01]), t_trig);
			chainA = FFT(LocalBuf(2048), inA);
			chainB = FFT(LocalBuf(2048), inB);
			chain = PV_RectComb2(chainA, chainB, 5, EnvGen.ar(Env.new([0, 1], [morphtime]), t_trig)*pi, 0.5); 
			Out.ar(out,  PanAz.ar(numChan, IFFT(chain), EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.3]), t_trig, doneAction:2), orientation: 0.0) * amp);
		}).add;

		// non-fft synthdef
		SynthDef(\low, { arg out=0, freq=261.63, panFrom=0, panTo=0, morphtime=1, amp=0.3, buf1, buf2, dur, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.02]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.02]), t_trig);
			chain = 	LPF.ar(inB, EnvGen.ar(Env.new([0.001, 1], [morphtime], 'exponential'), t_trig)*16000)+
					LPF.ar(inA, EnvGen.ar(Env.new([1, 0.001], [morphtime], 'exponential'), t_trig)*16000); 
			Out.ar(out,  PanAz.ar(numChan, chain, EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.07]), t_trig, doneAction:2), orientation: 0.0) * amp);
		}).add;

		SynthDef(\band, { arg out=0, freq=261.63, panFrom=0, panTo=0, morphtime=1, amp=0.3, buf1, buf2, dur, gate=1, t_trig, loop=1;
			var inA, chainA, inB, chainB, chain;
			inA = PlayBuf.ar(1, buf1, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.02]), t_trig);
			inB = PlayBuf.ar(1, buf2, (freq.cpsmidi-60).midiratio, loop: loop) * EnvGen.ar(Env.new([0, 1, 1, 0], [0.1, morphtime, 0.02]), t_trig);
			chain = 	BPF.ar(inB, EnvGen.ar(Env.new([0.01, 1], [morphtime], 'exponential'), t_trig)*10000)+
					BPF.ar(inA, EnvGen.ar(Env.new([1, 0.01], [morphtime], 'exponential'), t_trig)*10000); 
			Out.ar(out,  PanAz.ar(numChan, chain, EnvGen.ar(Env.new([panFrom, panTo], [morphtime+0.07]), t_trig, doneAction:2), orientation: 0.0) * amp);
		}).add;
		

		// ---------------------- synthesized instruments -----------------------------
		
		SynthDef(\impulse, { arg out=0, gate=1, pan=0, amp=1;
			var x, imp, killenv;
			killenv = EnvGen.ar(Env.adsr(0.0000001, 1, 0.2), gate, doneAction:2);
			imp = Impulse.ar(1);
			x = PanAz.ar(2, imp * EnvGen.ar(Env.perc(0.0000001, 0.2)), pan, orientation: 0.0) * amp;
			Out.ar(out, LeakDC.ar(Limiter.ar(x)));
		}).add(\xiilang);
		
		SynthDef(\kick,{ arg out=0, pan=0, amp=0.3, mod_freq = 2.6, mod_index = 5, sustain = 0.4, beater_noise_level = 0.025;
			var pitch_contour, drum_osc, drum_lpf, drum_env;
			var beater_source, beater_hpf, beater_lpf, lpf_cutoff_contour, beater_env;
			var kick_mix, freq = 80;
			pitch_contour = Line.kr(freq*2, freq, 0.02);
			drum_osc = PMOsc.ar(	pitch_contour,
						mod_freq,
						mod_index/1.3,
						mul: 1,
						add: 0);
			drum_lpf = LPF.ar(in: drum_osc, freq: 1000, mul: 1, add: 0);
			drum_env = drum_lpf * EnvGen.ar(Env.perc(0.005, sustain), 1.0, doneAction: 2);
			beater_source = WhiteNoise.ar(beater_noise_level);
			beater_hpf = HPF.ar(in: beater_source, freq: 500, mul: 1, add: 0);
			lpf_cutoff_contour = Line.kr(6000, 500, 0.03);
			beater_lpf = LPF.ar(in: beater_hpf, freq: lpf_cutoff_contour, mul: 1, add: 0);
			beater_env = beater_lpf * EnvGen.ar(Env.perc(0.000001, 1), doneAction: 2);
			kick_mix = Mix.new([drum_env, beater_env]) * 2 * amp;
			Out.ar(out, PanAz.ar(numChan, kick_mix, pan, orientation: 0.0))
		}).add(\xiilang);

		SynthDef(\kick2, {	arg out=0, amp=0.3, sustain=0.26, pan=0;
			var env0, env1, env1m, son;
			
			env0 =  EnvGen.ar(Env.new([0.5, 1, 0.5, 0], [0.005, 0.06, sustain], [-4, -2, -4]), doneAction:2);
			env1 = EnvGen.ar(Env.new([110, 59, 29], [0.005, 0.29], [-4, -5]));
			env1m = env1.midicps;
			
			son = LFPulse.ar(env1m, 0, 0.5, 1, -0.5) + WhiteNoise.ar(1);
			son = LPF.ar(son, env1m*1.5, env0);
			son = son + SinOsc.ar(env1m, 0.5, env0);
			
			son = son * 1.2;
			son = son.clip2(1);
			
			Out.ar(out, PanAz.ar(numChan, son * amp, pan, orientation: 0.0));
		}).add(\xiilang);
				
		SynthDef(\kick3, { arg out=0, amp=0.3, pan=0, dur=0.35, high=150, sustain = 0.4, low=33, phase=1.5;
			var signal;
			signal = SinOsc.ar(XLine.kr(high, low, dur), phase*pi, amp);
			//signal = signal * EnvGen.ar(Env.new([1,0],[dur]), gate, doneAction:2);
			signal = signal * EnvGen.ar(Env.perc(0.0001, sustain), doneAction:2);
			Out.ar(out, Pan2.ar(signal, pan));
		}).add(\xiilang);
		
		SynthDef(\snare, {arg out=0, amp=0.3, pan=0, sustain = 0.04, drum_mode_level = 0.15,
			snare_level = 50, snare_tightness = 1200, freq = 305;
			var drum_mode_sin_1, drum_mode_sin_2, drum_mode_pmosc, drum_mode_mix, drum_mode_env;
			var snare_noise, snare_brf_1, snare_brf_2, snare_brf_3, snare_brf_4, snare_reson;
			var snare_env, snare_drum_mix;
		
			drum_mode_env = EnvGen.ar(Env.perc(0.005, sustain), doneAction: 2);
			drum_mode_sin_1 = SinOsc.ar(freq*0.53, 0, drum_mode_env * 0.5);
			drum_mode_sin_2 = SinOsc.ar(freq, 0, drum_mode_env * 0.5);
			drum_mode_pmosc = PMOsc.ar(	Saw.ar(freq*0.85),
							184,
							0.5/1.3,
							mul: drum_mode_env*5,
							add: 0);
			drum_mode_mix = Mix.new([drum_mode_sin_1, drum_mode_sin_2, drum_mode_pmosc]) * drum_mode_level;
		// choose either noise source below
		//	snare_noise = WhiteNoise.ar(amp);
			snare_noise = LFNoise0.ar(9000, amp*0.8); // play with the frequency here
			snare_env = EnvGen.ar(Env.perc(0.0001, sustain), doneAction: 2);
			snare_brf_1 = BRF.ar(in: snare_noise, freq: 8000, mul: 0.5, rq: 0.1);
			snare_brf_2 = BRF.ar(in: snare_brf_1, freq: 5000, mul: 0.5, rq: 0.1);
			snare_brf_3 = BRF.ar(in: snare_brf_2, freq: 3600, mul: 0.5, rq: 0.1);
			snare_brf_4 = BRF.ar(in: snare_brf_3, freq: 2000, mul: snare_env, rq: 0.1);
			snare_reson = Resonz.ar(snare_brf_4, snare_tightness, mul: snare_level) ;
			snare_drum_mix = Mix.new([drum_mode_mix, snare_reson]) * amp;
			Out.ar(out, PanAz.ar(numChan, snare_drum_mix, pan, orientation: 0.0))
		}).add(\xiilang);
				
		SynthDef(\brushsnare, {|out= 0, bpfreq= 5000, amp= 1, pan= 0|
			var env, noise;
			env = EnvGen.kr(Env.perc(0.001, 0.1), 1, amp, doneAction:2);
			noise = BPF.ar(PinkNoise.ar(3), bpfreq * (env*8.5));
			Out.ar(out, PanAz.ar(numChan, noise*env, pan, orientation: 0.0));
		}).add(\xiilang);
		
		SynthDef(\bar, {arg out = 0, pan=0, freq = 6000, sustain = 0.2, amp=0.3;
			var root_cymbal, root_cymbal_square, root_cymbal_pmosc;
			var initial_bpf_contour, initial_bpf, initial_env;
			var body_hpf, body_env;
			var cymbal_mix;
			
			root_cymbal_square = Pulse.ar(freq, 0.5, mul: 0.81);
			root_cymbal_pmosc = PMOsc.ar(root_cymbal_square,
							[freq*1.34, freq*2.405, freq*3.09, freq*1.309],
							[310/1.3, 26/0.5, 11/3.4, 0.72772],
							mul: 1,
							add: 0);
			root_cymbal = Mix.new(root_cymbal_pmosc);
			initial_bpf_contour = Line.kr(15000, 9000, 0.1);
			initial_env = EnvGen.ar(Env.perc(0.005, 0.1), 1.0);
			initial_bpf = BPF.ar(root_cymbal, initial_bpf_contour, mul:initial_env);
			body_env = EnvGen.ar(Env.perc(0.005, sustain, 1, -2), doneAction: 2);
			body_hpf = HPF.ar(in: root_cymbal, freq: Line.kr(9000, 12000, sustain),mul: body_env, add: 0);
			cymbal_mix = Mix.new([initial_bpf, body_hpf]) * amp;
			Out.ar(out, PanAz.ar(numChan, cymbal_mix, pan, orientation: 0.0))
		}).add(\xiilang);

		SynthDef(\clap, {arg out=0, pan=0, amp=0.3, filterfreq=50, rq=0.01;
			var env, signal, attack,Ê noise, hpf1, hpf2;
			noise = WhiteNoise.ar(1)+SinOsc.ar([filterfreq/2,filterfreq/2+4 ], pi*0.5, XLine.kr(1,0.01,4));
			//noise = PinkNoise.ar(1)+SinOsc.ar([(filterfreq)*XLine.kr(1,0.01,3), (filterfreq+4)*XLine.kr(1,0.01,3) ], pi*0.5, XLine.kr(1,0.01,4));
			//signal = signal * SinOsc.ar(1,0.75);
			hpf1 = RLPF.ar(noise, filterfreq, rq);
			hpf2 = RHPF.ar(noise, filterfreq/2, rq/4);
			env = EnvGen.kr(Env.perc(0.003, 0.00035));
			signal = (hpf1+hpf2) * env;
			signal = CombC.ar(signal, 0.5, 0.03, 0.031)+CombC.ar(signal, 0.5, 0.03016, 0.06);
			//signal = Decay2.ar(signal, 0.5);
			signal = FreeVerb.ar(signal, 0.23, 0.15, 0.2);
			Out.ar(out, PanAz.ar(numChan, signal * amp, pan, orientation: 0.0));
			DetectSilence.ar(signal, doneAction:2);
		}).add(\xiilang);
		
		SynthDef(\hat, { arg out=0, pan=0, amp=0.3;
			var sig;
			// a release gate
			EnvGen.ar(Env.perc(0.00001, 2), doneAction: 2); 
			// the other env has problem with gate
			// (i.e. FAILURE n_set Node not found)
			sig = WhiteNoise.ar(amp) * EnvGen.ar(Env.perc(0.00001, 0.01));
			Out.ar(out, PanAz.ar(numChan, sig, pan, orientation: 0.0));
		}).add(\xiilang);

//		SynthDef(\impulse, { // no amp atm
//			Out.ar(0, Impulse.ar(0)!2)
//		}).add;
//


		// ----------------------------------------------------------------------------------
		// ------------------------------- melodic instruments  -----------------------------
		// ----------------------------------------------------------------------------------

/*
// a pattern to test the instruments
Pdef(\test, Pbind(\instrument, \clap, \midinote, Prand([1, 2, 5, 7, 9, 3], inf) + 60, \dur, 0.8)).play;
*/

		SynthDef(\whistle, {arg out=0, freq=130.8128, gate=1, amp=0.3, dur=1;
			var signal, env;
			env = EnvGen.ar(Env.asr(0.01, 1, 0.2), gate, doneAction:2);
			signal = GVerb.ar(SyncSaw.ar(freq, XLine.ar(freq*2, freq*4, dur/2), amp*0.1));
			signal = signal + LPF.ar(PinkNoise.ar(amp), XLine.ar(60.midicps, 72.midicps, dur/2));
			Out.ar(out, signal*env*amp);
		}).add(\xiilang);

		SynthDef(\dubbass, {arg out=0, freq=440, dur=1, gate=1, amp=0.3, target=8;
			var saw, env;
			saw = SyncSaw.ar([freq, freq+1], [Line.ar(freq, freq*target, dur+0.2), Line.ar(freq, freq*target, dur+0.2)+1], amp);
			saw = RLPF.ar(saw, XLine.ar(freq*4, freq*9, dur), XLine.ar(0.9, 0.1, dur));
			env = EnvGen.ar(Env.asr(0.01, 1, 0.2), gate, doneAction:2);
			Out.ar(out, saw*env*amp); // should really use an env here, but it sound cool without it
		}).add(\xiilang);

		SynthDef(\bengaX, {arg out=0, freq=440, dur=1, gate=1, amp=0.3;
			var saw, filter, env;
			saw = SinOsc.ar([1, 1.01]*SinOsc.ar(223).range(freq/4, freq/2), amp)+LPF.ar(Saw.ar([freq, freq+1]/2, 1), SinOsc.ar(5).range(freq, freq*3))+LPF.ar(Saw.ar([freq, freq+1]/4, amp), LFNoise2.ar(3).range(freq, freq*3));
			filter = MoogFF.ar(saw, freq*22, 1);
			env = EnvGen.ar(Env.asr(0.01, amp, 0.2), gate, doneAction:2);
			Out.ar(out, filter*env);
		}).add(\xiilang);

		SynthDef(\dubpad, {arg out=0, freq=220, gate=1, amp=0.3;
			var wave, amps, env;
			amps = [0.6134, 0.5103, 0.3041, 0.2216, 0.4175, 0.1082, 0.067, 0.0773, 0, 0.01546];
			wave = amps.collect({|amp, i| SinOsc.ar([freq *(i+1), freq *(i+1) +Rand(1, 3.8)], 0, amp*0.1) }).sum;
			env = EnvGen.ar(Env.asr(0.01, 1, 0.2), gate, doneAction:2);
			Out.ar(out, wave*amp*env);
		}).add(\xiilang);
		
		//Synth(\dubpad, [\freq, 188, \amp, 1])
		
		
		SynthDef(\dubchordpad, {arg out=0, freq=220, gate=1, amp=0.3, pan=0;
			var wave1, wave2, wave3, amps, env;
			amps = [0.6134, 0.5103, 0.3041, 0.2216, 0.4175, 0.1082, 0.067, 0.0773, 0, 0.01546];
			wave1 = amps.collect({|amp, i| SinOsc.ar([freq *(i+1), freq *(i+1) +Rand(1, 3.8)], 0, amp*0.1) }).sum;
			wave2 = amps.collect({|amp, i| SinOsc.ar([freq*1.1892 *(i+1), freq*1.1892 *(i+1) +Rand(1, 3.8)], 0, amp*0.1) }).sum;
			wave3 = amps.collect({|amp, i| SinOsc.ar([freq*1.5 *(i+1), freq*1.5 *(i+1) +Rand(1, 3.8)], 0, amp*0.1) }).sum;
			env = EnvGen.ar(Env.asr(0.01, 1, 0.2), gate, doneAction:2);
			Out.ar(out, (wave1+wave2+wave3)*amp*env);
		}).add(\xiilang);

		SynthDef(\deepdubsynth, {arg out=0, freq=220, amp=0.1, dur=1, gate=1, tmp=2;
			var trig, note, son, sweep, bassenv, bd, sd, swr;
			var midinote, unienv;
		ÊÊÊ	trig = Impulse.kr(tmp);
			
			midinote = freq.cpsmidi/2;
			
			note = Demand.kr(trig, 0, Dxrand([midinote, midinote+(Rand(10)/10).round, midinote+(Rand(30)/10).round, midinote+(Rand(40)/10).round, midinote+(Rand(30)/10).round].midicps, inf));
			swr = Demand.kr(trig, 0, Drand([0.5, 1, 2, 3, 4, 6], inf));
		
			sweep = LFTri.ar(swr).exprange(140, 10000);
			//x = LFNoise2.ar(swr).exprange(140, 10000);
			son = LFSaw.ar(note *[0.99, 1, 1.01] ).sum;
			son = LPF.ar(son, sweep);	
			son = son.sin.tanh;
		//	y = Saw.ar(note *[0.99, 1, 1.01] ).sum;
		//	y = RLPF.ar(son, x, MouseX.kr(0.1, 0.9));	
		//	son+y
			 unienv = EnvGen.ar(Env.asr(0.01, amp*0.4, 0.2), gate, doneAction:2);
			Out.ar(out, unienv*son!2);
		}).add(\xiilang);

		SynthDef(\casp, {arg out=0, freq=220, amp=0.1, dur=1, gate=1;
			var son, unienv;
			son = Pulse.ar(freq * [1, 1.01], 0.5 , amp).sum + Pulse.ar(freq * [1, 1.01] *0.5, 0.5, 0.2 ).sum;
			//son = RLPF.ar(son, freq*10, 0.01);
			unienv = EnvGen.ar(Env.asr(0.01, amp*0.3, 0.2), gate, doneAction:2);
			Out.ar(out, unienv*son!2);
		}).add(\xiilang);
		
		SynthDef(\caspRLPF, {arg out=0, freq=220, amp=0.1, dur=1, gate=1;
			var son, unienv;
			son = Pulse.ar(freq * [1, 1.01], 0.5, amp ).sum + Pulse.ar(freq * [1, 1.01] *0.5, 0.5, 0.2 ).sum;
			son = RLPF.ar(son, freq*10, 0.01);
			unienv = EnvGen.ar(Env.asr(0.01, amp*0.3, 0.2), gate, doneAction:2);
			Out.ar(out, unienv*son!2);
		}).add(\xiilang);


		SynthDef(\cling, {arg out=0, amp=0.3, sustain=0.3, pan=0;
			var signal, env;
			env = EnvGen.ar(Env.perc(0.000001, sustain), doneAction:2);
			signal = SinOsc.ar(3000).squared;
			Out.ar(out, PanAz.ar(numChan, signal*env, pan, amp, orientation: 0.0));
		}).add(\xiilang);
		
		SynthDef(\cling2, {arg out=0, amp=0.3, sustain=0.5, pan=0;
			var signal, env;
			env = EnvGen.ar(Env.perc(0.000001, sustain), doneAction:2);
			signal = LFSaw.ar(2000).squared;
			Out.ar(out, PanAz.ar(numChan, signal*env, pan, amp, orientation: 0.0));
		}).add(\xiilang);

		SynthDef(\flute, { arg out=0, scl=0.2, freq=440, ipress=0.9, ibreath=0.09, ifeedbk1=0.4, ifeedbk2=0.4, sustain=0.15, gate=1, amp=1, pan=0;
			var kenv1, kenv2, kenvibr, kvibr, sr, cr, block;
			var poly, signalOut, ifqc;
			var aflow1, asum1, asum2, afqc, atemp1, ax, apoly, asum3, avalue, atemp2, aflute1;
			var fdbckArray;
			
			sr = SampleRate.ir;
			cr = ControlRate.ir;
			block = cr.reciprocal;
			ifqc = freq;
			// noise envelope
			kenv1 = EnvGen.kr(Env.new( 
				[ 0.0, 1.1 * ipress, ipress, ipress, 0.0 ], [ 0.06, 0.2, 8 - 0.46, 0.2 ], 'linear' )
			);
			kenv2 = EnvGen.kr(Env.adsr(0.0001, 0.1, 1, 0.3), gate, doneAction:2);

/*			// overall envelope
			kenv2 = EnvGen.kr(Env.new(
				[ 0.0, amp, amp, 0.0 ], [ 0.1, sustain - 0.02, 0.1 ], 'linear' ), doneAction: 2 
			);
*/
			// vibrato envelope
			kenvibr = EnvGen.kr(Env.new( [ 0.0, 0.0, 1, 1, 0.0 ], [ 0.5, 0.5, 8 - 1.5, 0.5 ], 'linear') );
			// create air flow and vibrato
			aflow1 = LFClipNoise.ar( sr, kenv1 );
			kvibr = SinOsc.ar( 5, 0, 0.1 * kenvibr );
			asum1 = ( ibreath * aflow1 ) + kenv1 + kvibr;
			afqc = ifqc.reciprocal - ( asum1/20000 ) - ( 9/sr ) + ( ifqc/12000000 ) - block;
			fdbckArray = LocalIn.ar( 1 );
			aflute1 = fdbckArray;
			asum2 = asum1 + ( aflute1 * ifeedbk1 );
			//ax = DelayL.ar( asum2, ifqc.reciprocal * 0.5, afqc * 0.5 );
			ax = DelayC.ar( asum2, ifqc.reciprocal - block * 0.5, afqc * 0.5 - ( asum1/ifqc/cr ) + 0.001 );
			apoly = ax - ( ax.cubed );
			asum3 = apoly + ( aflute1 * ifeedbk2 );
			avalue = LPF.ar( asum3, 2000 );
			aflute1 = DelayC.ar( avalue, ifqc.reciprocal - block, afqc );
			fdbckArray = [ aflute1 ];
			LocalOut.ar( fdbckArray );
			signalOut = avalue * amp;
			OffsetOut.ar(out, PanAz.ar(numChan, signalOut * kenv2, pan, orientation: 0.0) );
		}).add(\xiilang);

		// ---------------------- synthesized instruments -----------------------------
		// a crappy synth as to yet
		SynthDef(\fmsynth, {|out=0, freq=440, carPartial=1, modPartial=1.5, index=13, gate=1, amp=0.3, pan=0|
			var mod, car, env;
			// modulator frequency
			mod = SinOsc.ar(freq * modPartial, 0, freq * index );
			// carrier frequency
			car = SinOsc.ar((freq * carPartial) + mod, 0, amp );
			// envelope
			env = EnvGen.kr(Env.adsr, gate, doneAction:2);
			Out.ar( out, PanAz.ar(numChan, (car * env * amp)!2, pan, orientation: 0.0))
		}).add(\xiilang);


		SynthDef(\bling, { arg out=0, pan=0, amp=0.3, sustain=0.5, freq=999;
			var x, y, env, imp;
			env = Env.perc(0.0000001, sustain*2);
			imp = Impulse.ar(1);
			imp = Decay2.ar(imp, 0.01, 0.5, MoogFF.ar(VarSaw.ar(freq, 0.8, 0.5), freq*12, 3.6) );
			x = PanAz.ar(numChan, imp * EnvGen.ar(env, doneAction:2), pan, orientation: 0.0) * amp*4;
			Out.ar(out, LeakDC.ar(x));
		}).add(\xiilang);

		/*
		SynthDef(\bass, {arg out, freq=220, amp=0.4;
			var env, signal;
			env = EnvGen.ar(Env.perc(0.01,2), doneAction:2);
			signal = SinOsc.ar([freq/2, (freq/2)+2], 0, amp) * env;
			Out.ar(out, signal*env);
		}).add;
		*/
		
		SynthDef(\bass, {arg out, freq=220, gate=1, sustain=0.3, amp=0.3;
			var env, signal;
			env = EnvGen.ar(Env.adsr(0.01, sustain, sustain/2, 0.3), gate, doneAction:2);
			signal = MoogFF.ar(Saw.ar([freq/2, (freq/2)+0.8],  amp*2), freq*2, 3.4) * env;
			Out.ar(out, signal*env);
		}).add(\xiilang);


		/*
		 Synth(\bass, [\freq, 344])
		*/

/*		
		SynthDef(\moog, {arg out=0, freq=220, amp=0.3, sustain=0.3, gate=1;
			var signal;
			var env = EnvGen.kr(Env.adsr(0.01, sustain, sustain/2, 0.3), gate, doneAction:2);
			signal = MoogFF.ar(Saw.ar([freq, freq+2], amp), 7*freq, 3.3) * env * amp;
			Out.ar(out, signal);
		}).add;

*/
		SynthDef(\moog, {arg out=0, freq=220, amp=0.3, sustain=0.3, gate=1;
			var signal;
			var env = EnvGen.kr(Env.adsr(0.01, 0.2, amp*0.8, 0.3), gate, doneAction:2);
			signal = MoogFF.ar(Saw.ar([freq, freq+2], 1), 7*freq, 3.3) * env;
			Out.ar(out, signal);
		}).add(\xiilang);
		/*
		 Synth(\moog, [\freq, 344])
		*/
		
		SynthDef(\bell, {arg out=0, freq=440, sustain=0.5, amp=0.3, pan=0;
		        var x, in, env;
		        env = EnvGen.kr(Env.perc(0.01, sustain*Rand(333, 666)/freq), doneAction:2);
		        x = Mix.ar([SinOsc.ar(freq, 0, 0.11), SinOsc.ar(freq*2, 0, 0.09)] ++
		        				Array.fill(6, {SinOsc.ar(freq*Rand(-5,5).round(0.125), 0, Rand(0.02,0.1))}));
		        //x = BPF.ar(x, freq, 4.91);
		        Out.ar(out, PanAz(6, x!2*env*amp, pan, orientation: 0.0));
		}).add(\xiilang);
		/*
		 Synth(\bell, [\freq, 344])
		*/
		
		// rubbish! MdaPiano does not support microtonality !!! 
		SynthDef(\piano, { |out=0, freq=440, gate=1, sustain = 0.9, amp=0.3|
			var sig = MdaPiano.ar(freq, gate, decay:(sustain*2), release: (sustain*6), stereo: 0.3, sustain: 0);
			var env = EnvGen.kr(Env.adsr(0.01, sustain*4, sustain*2, 0.3), gate, doneAction:2);
			Out.ar(out, sig * (amp*0.35)*env);
		}).add(\xiilang);

		SynthDef(\piano, { |out=0, freq=440, gate=1, sustain = 0.9, amp=0.3|
			var sig = MdaPiano.ar(freq, gate, decay:(sustain*2), release: (sustain*6), stereo: 0.3, sustain: 0);
			var env = EnvGen.kr(Env.adsr(0.01, sustain*4, sustain*2, 0.3), gate, doneAction:2);
			Out.ar(out, sig * (amp*0.35)*env);
		}).add;


		SynthDef(\clarinet, { |out=0, freq=440, gate=1, sustain=0.3, amp=0.3|
			var sig = StkClarinet.ar(freq, 44, 2, 77, 2, 88);
			var env = EnvGen.kr(Env.adsr(0.01, sustain, sustain/2, 0.3), gate, doneAction:2);
			Out.ar(out, sig * env * amp * 0.6 !2 );
		}).add(\xiilang);

		SynthDef(\noise, {arg out=0, freq=440, amp=0.3, sustain=0.3, pan=0, gate=1;
			var signal, env;
			env = EnvGen.kr(Env.adsr(0.01, sustain, 1, 0.3), gate, doneAction:2);
			signal = BPF.ar(PinkNoise.ar(1), freq, 0.008, 100);
			Out.ar(out, Pan2.ar(signal*env, pan, amp));
		}).add(\xiilang);

		SynthDef(\fmbass, {arg out=0, freq=440, amp=0.3, sustain=0.3, pan=0, gate=1;
			var signal, env;
			env = EnvGen.kr(Env.adsr(0.01, sustain, 1, 0.3), gate, doneAction:2);
			signal = SinOsc.ar(SinOsc.ar(freq*0.5, 0, 500*XLine.kr(0.5, 1, sustain)));
			Out.ar(out, Pan2.ar(signal*env, pan, amp));
		}).add(\xiilang);

		SynthDef(\klang, {arg out=0, amp=0.3, t_trig=1, sustain=0.4, freq=100, gate=1, rq=0.004;
			var env, signal;
			var rho, theta, b1, b2;
			env = EnvGen.kr(Env.adsr(0.01, sustain, sustain/2, 0.3), gate, doneAction:2);
			b1 = 2.0 * 0.97576 * cos(0.161447);
			b2 = 0.9757.squared.neg;
			signal = SOS.ar(K2A.ar(t_trig), 1.0, 0.0, 0.0, b1, b2);
			signal = RHPF.ar(signal, freq, rq);
			signal = Decay2.ar(signal, 0.4, 0.8, signal);
			signal = Limiter.ar(Resonz.ar(signal, freq, rq*0.5), 0.9);
			Out.ar(out, (signal*env)*(amp*6)!2);
		}).add(\xiilang);

		SynthDef(\elbass, {arg out=0, amp=0.3, t_trig=1, sustain=0.5, freq=100, gate=1, rq=0.004;
			var env, signal;
			var rho, theta, b1, b2;
			env = EnvGen.kr(Env.adsr(0.0001, sustain, sustain/2, 0.3), gate, doneAction:2);
			b1 = 1.98 * 0.989999999 * cos(0.09);
			b2 = 0.998057.neg;
			signal = SOS.ar(K2A.ar(t_trig), 0.123, 0.0, 0.0, b1, b2);
			signal = RHPF.ar(signal, freq, rq) + RHPF.ar(signal, freq*0.5, rq);
			signal = Decay2.ar(signal, 0.4, 0.3, signal);
			Out.ar(out, (signal*env)*(amp*0.65)!2);
		}).add(\xiilang);

		SynthDef(\marimba, {arg out=0, amp=0.3, t_trig=1, sustain=0.5, gate=1, freq=100, rq=0.006;
			var env, signal;
			var rho, theta, b1, b2;
			env = EnvGen.kr(Env.adsr(0.0001, sustain, sustain/2, 0.3), gate, doneAction:2);
			b1 = 1.987 * 0.9889999999 * cos(0.09);
			b2 = 0.998057.neg;
			signal = SOS.ar(K2A.ar(t_trig), 0.3, 0.0, 0.0, b1, b2);
			signal = RHPF.ar(signal*0.8, freq, rq) + DelayC.ar(RHPF.ar(signal*0.9, freq*0.99999, rq*0.999), 0.02, 0.01223);
			signal = Decay2.ar(signal, 0.4, 0.3, signal);
			Out.ar(out, (signal*env)*(amp*0.65)!2);
		}).add(\xiilang);

		SynthDef(\marimba2, {arg out=0, amp=0.3, t_trig=1, freq=100, sustain=0.5, gate=1, rq=0.006;
			var env, signal;
			var rho, theta, b1, b2;
			env = EnvGen.kr(Env.adsr(0.0001, sustain, sustain/2, 0.3), gate, doneAction:2);
			b1 = 1.987 * 0.9889999999 * cos(0.09);
			b2 = 0.998057.neg;
			signal = SOS.ar(K2A.ar(t_trig), 0.3, 0.0, 0.0, b1, b2);
			signal = RHPF.ar(signal*0.8, freq, rq) + DelayC.ar(RHPF.ar(signal*0.9, freq*0.99999, rq*0.999), 0.02, 0.018223);
			//signal = Decay2.ar(signal, 0.4, 0.3, signal);
			signal = Decay2.ar(signal, 0.4, 0.3, signal*SinOsc.ar(freq)); // modulating
			Out.ar(out, (signal*env)*(amp*0.65)!2);
		}).add(\xiilang);

		SynthDef(\wood, {arg out=0, amp=0.3, pan=0, sustain=0.5, t_trig=1, freq=100, rq=0.06;
			var env, signal;
			var rho, theta, b1, b2;
			b1 = 2.0 * 0.97576 * cos(0.161447);
			b2 = 0.9757.squared.neg;
			signal = SOS.ar(K2A.ar(t_trig), 1.0, 0.0, 0.0, b1, b2);
			//signal = RHPF.ar(signal, freq, rq);
			signal = Decay2.ar(signal, 0.4, 0.8, signal);
			signal = Limiter.ar(Resonz.ar(signal, freq, rq*0.5), 0.9);
			env = EnvGen.kr(Env.perc(0.00001, sustain, amp), doneAction:2);
			Out.ar(out, PanAz.ar(numChan, signal, pan, orientation: 0.0)*env);
		}).add(\xiilang);

		SynthDef(\xylo, { |out=0, freq=440, gate=1, amp=0.3, sustain=0.5, pan=0|
			var sig = StkBandedWG.ar(freq, instr:1, mul:3);
			var env = EnvGen.kr(Env.adsr(0.0001, sustain, sustain, 0.3), gate, doneAction:2);
			Out.ar(out, PanAz.ar(numChan, sig, pan, env * amp, orientation: 0.0));
		}).add(\xiilang);

		SynthDef(\softwg, { |out=0, freq=440, gate=1, amp=0.3, sustain=0.5, pan=0|
			var sig = StkBandedWG.ar(freq, instr:1, mul:3);
			var env = EnvGen.kr(Env.adsr(0.0001, sustain, sustain, 0.3), gate, doneAction:2);
			Out.ar(out, PanAz.ar(numChan, sig, pan, env*amp, orientation: 0.0));
		}).add(\xiilang);

		SynthDef(\sines, {arg out=0, freq=440, dur=1, sustain=0.5, amp=0.3, pan=0;
		        var x, env;
		        env = EnvGen.kr(Env.perc(0.01, sustain, amp), doneAction:2);
		        x = Mix.ar(Array.fill(8, {SinOsc.ar(freq*IRand(1,10),0, 0.08)}));
		        x = LPF.ar(x, 20000);
		        x = PanAz.ar(numChan, x,pan, orientation: 0.0);
		        Out.ar(out, x*env);
		}).add(\xiilang);
		
		SynthDef(\synth, {arg out=0, freq=440, dur=1, sustain=0.5, amp=0.3, pan=0, gate=1;
		        var x, env;
			   env = EnvGen.kr(Env.adsr(0.0001, sustain, sustain*0.8, 0.3), gate, doneAction:2);
		        x = Mix.ar([FSinOsc.ar(freq, pi/2, 0.5), Pulse.ar(freq, Rand(0.3,0.5), 0.5)]);
		        x = LPF.ar(x, 20000);
		        x = PanAz.ar(numChan, x,pan, orientation: 0.0);
		        Out.ar(out, LeakDC.ar(x)*env*amp*0.8);
		}).add(\xiilang);

		SynthDef(\string, {arg out=0, freq=440, pan=0, sustain=0.5, amp=0.3;
			var pluck, period, string;
			pluck = PinkNoise.ar(Decay.kr(Impulse.kr(0.005), 0.05));
			period = freq.reciprocal;
			string = CombL.ar(pluck, period, period, sustain*6);
			string = LeakDC.ar(LPF.ar(PanAz.ar(numChan, string, pan, orientation: 0.0), 12000)) * amp;
			DetectSilence.ar(string, doneAction:2);
			Out.ar(out, string)
		}).add(\xiilang);

		SynthDef(\drop, {arg out=0, freq=440, dur=1, amp=0.3, sustain=0.5, pan=0;
		        var x, env;
		        env = EnvGen.kr(Env.perc(0.001, sustain*1.5), doneAction:2);
		        x = Resonz.ar(PinkNoise.ar(1), freq*4, 0.005);
		        x = PanAz.ar(numChan, x,pan, orientation: 0.0);
		        Out.ar(out, LeakDC.ar(x)*env*amp*70);
		}).add(\xiilang);
		
		SynthDef(\crackle, {arg out=0, freq=440, dur=1, sustain=0.5, amp=0.3, pan=0;
		        var x, env;
		        env = EnvGen.kr(Env.perc(0.01, sustain), doneAction:2);
		        x = Resonz.ar(Crackle.ar(1.95, 2), freq*4, 0.1);
		        x = PanAz.ar(numChan, x,pan, orientation: 0.0);
		        Out.ar(out, LeakDC.ar(x)*env*amp*8);
		}).add(\xiilang);

		SynthDef(\glass, {arg out=0, freq=440, dur=1, sustain=0.5, amp=0.3, pan=0;
		        var x, env;
		        env = EnvGen.kr(Env.perc(0.0001, sustain*4), doneAction:2);
		        x = Decay2.ar(Resonz.ar(Impulse.ar(0.01), freq*4, 0.005), 0.001, sustain*2, 3);
		        x = PanAz.ar(numChan, x,pan, orientation: 0.0);
		        Out.ar(out, LeakDC.ar(x)*env*amp*50);
		}).add(\xiilang);

		SynthDef(\sine, {arg out=0, gate=1, freq=440, dur=1, sustain=0.5, amp=0.3, pan=0;
			var env = EnvGen.kr(Env.adsr(0.0001, sustain, sustain/2, 0.3), gate, doneAction:2);
			Out.ar(out, PanAz.ar(numChan, SinOsc.ar(freq), pan, env * amp, orientation: 0.0));
		}).add(\xiilang);


		SynthDef(\XiiRecordBuf, {arg inbus = 8, bufnum = 0;
			var input;
			input = In.ar(inbus, 1);
			RecordBuf.ar(input, bufnum);
		}).add(\xiilang);

		//{SynthDescLib.all[\xiilang].read; SynthDescLib.read}.defer(2);

		//^this.makeInstrDict; // changed such that the class returns its instance (not the dict)

	}
	
	makeInstrDict{ // this is where keys are mapped to instruments (better done by hand and design)
	
		// if sounds folder contains a key mapping file, then it is used, 
		// else, the instrDict is created by mapping random sound files onto the letters
		
		var file;
		if(Object.readArchive("ixilang/"++project++"/keyMapping.ixi").isNil, {
			instrDict = IdentityDictionary.new;
			[\A, \a, \B, \b, \C, \c, \D, \d, \E, \e, \F, \f, \G, \g, \H, \h, \I, \i, \J, \j,
			\K, \k, \L, \l, \M, \m, \N, \n, \O, \o, \P, \p, \Q, \q, \R, \r, \S, \s, \T, \t,
			\U, \u, \V, \v, \W, \w, \X, \x, \Y, \y, \Z, \z].do({arg letter, i;
				instrDict[letter] = sampleNames.wrapAt(i).asSymbol;
			});
			" --->    ixi lang : No key mappings were found, so samples will be randomly assigned to keys - see helpfile, or type 'new' and map the keys".postln;
		}, {
			instrDict = Object.readArchive("ixilang/"++project++"/keyMapping.ixi");
		});
		
		"The keys of your keyboard are mapped to the following samples :".postln;
		Post << this.getSamplesSynthdefs;
		if(sampleNames.size == 0, {
			"There were no samples in your samples folder, please put some there!".postln;
		});
		^instrDict;		
	
	}
	
	createRecorderDoc {arg caller; // doccolor, oncolor, inbus=8;
		var doc, buffer, duration, recsynth, recording = false;
		if(("ixilang/"++project++"/livesamples").pathMatch==[], {
			("mkdir -p" + ("ixilang/"++project++"/livesamples")).unixCmd; // create the samples folder
			"ixi-lang NOTE: a live samples folder was not found for saving scores - It was created".postln;
		});	
		"CREATING RECORDER".postln;
		doc = Document.new;		
		buffer = Buffer.alloc(Server.default, Server.default.sampleRate * 10.0, 1);
		doc.bounds_(Rect(20,230, 500, 400));
		doc.background_(caller.doccolor);
		doc.stringColor_(caller.oncolor);
		doc.font_(Font("Monaco",20));
		doc.promptToSave_(false);		
		doc.string_("Press any key to record its sound : \n\n");
		doc.keyDownAction_({|doc, char, mod, unicode, keycode| 
			// record into buffer
			if(recording == false, {
				recsynth = Synth(\XiiRecordBuf, [\inbus, caller.inbus, \bufnum, buffer.bufnum]);
				duration = Main.elapsedTime;
				recording = true;
				"STARTING RECORDING".postln;
			});
		});
		doc.keyUpAction_({|doc, char, mod, unicode, keycode | 
			var tempbuf, cond;
			if((mod == 8388864).not, {
				doc.string_(doc.string++"\n\nSound recorded into the key! \n\n Bye bye!");
				// save it to a file
				duration = Main.elapsedTime - duration;
				[\duration, duration].postln;
				cond = Condition.new;
				Routine.run {
					recsynth.free; // stop recording
					tempbuf = Buffer.alloc(Server.default, duration * Server.default.sampleRate, 1);
					Server.default.sync(cond);
					buffer.copyData(tempbuf, 0, 0,  duration * Server.default.sampleRate);
					Server.default.sync(cond);
					tempbuf.write(("ixilang/"++project++"/livesamples/"++char++".aif"), "aiff", "int16");
					"WRITING BUFFER".postln;
					this.makeSynthDef(tempbuf, char);
					caller.updateInstrDict(char);
				
				//	instrDict[char.asSymbol] = char.asSymbol; // instrDict[\a] = \a
				};
				doc.keyDownAction_({nil});
				{doc.close}.defer(2);
			});
		});
		
	}
	
	makeSynthDef { arg buffer, char;
		SynthDef(char.asSymbol, {arg out=0, freq=261.63, amp=0.3, pan=0, noteamp=1, sustain=0.4;
			var player, signal;
			bufferPool = bufferPool.add(buffer);
			// morpher instruments won't be stereo, so I just read one channel
			//bufferDict[sampleNames.wrapAt(i).asSymbol] = Buffer.readChannel(Server.default, filepath, channels: [0]);
			player = Select.ar(noteamp,
				[ // playMode 2 - the sample player mode
				LoopBuf.ar(1, buffer, (freq.cpsmidi-60).midiratio, 1, 0, 0, 44100*60*10) 
				* EnvGen.ar(Env.linen(0.0001, 60*60, 0.0001))
				, // playMode 1 - the rhythmic mode
				PlayBuf.ar(1, buffer, (freq.cpsmidi-60).midiratio) 
				* EnvGen.ar(Env.perc(0.01, sustain))
				]);
			
			// I use DetectSilence rather than doneAction in Env.perc, as a doneAction in Env.perc
			// would also be running (in Select) thus killing the synth even in {} mode
			// I therefore add 0.02 so the 
			DetectSilence.ar(player, 0.001, 0.5, 2);
			//signal = player * amp * Lag.kr(noteamp, dur); // works better without lag
			signal = player * amp * noteamp;
			Out.ar(out, Pan2.ar(signal, pan));
		}).add;
		
	}
	
	
	getSamplesSynthdefs {
		var string, sortedkeys, sortedvals;
		sortedkeys = instrDict.keys.asArray.sort;
		sortedvals = instrDict.atAll(instrDict.order);
		string = " ";
		sortedkeys.do({arg item, i; 
			string = string++item++"  :  "++sortedvals[i]++"\n"++" ";
		});
		^string;
	}

	returnPercussiveInstr {
		^instrDict.atAll(instrDict.order);
	}

	getXiiLangSynthesisSynthdefs {
		^SynthDescLib.getLib(\xiilang).synthDescs.keys.asArray.sort;
	}

	getProjectSynthesisSynthdefs {
		^SynthDescLib.getLib(project.asSymbol).synthDescs.keys.asArray.sort;
	}
	
	returnMelodicInstr {
		^SynthDescLib.getLib(project.asSymbol).synthDescs.keys.asArray
			++ 
		SynthDescLib.getLib(\xiilang).synthDescs.keys.asArray;
	}

	freeBuffers {
		bufferPool.do({arg buffer; buffer.free;});	
	}

	returnBufferDict {
		^bufferDict;	
	}
	
	returnInstrDict {
		^instrDict;
	}
}

		
	/*
	// code used to generate the initial keymappings file (now defunct, since the GUI)
	// map the keys to the names of the soundfiles inside your project folder
	// the project folder is the name of your session (so you start XiiLang("projectname")
	
	/*
	INSTRUCTIONS:
	1) Put this code into a document together with your sound samples in the project folder
	2) Enter the names of your soundfiles mapped to the keys (without the file ending)
	3) Highlight all (Apple + a)
	4) Hit SHIFT + RETURN (this will generate the keyMappings.ixi file in your project folder)
	*/


		var instrDict, project; // project is the folder name
		
		instrDict = IdentityDictionary.new;
		project = Document.current.dir.split.last;
		
		instrDict[\A] = \heartmachine;	
		instrDict[\a] = \heart;
				
		instrDict[\B] = \blade;		
		instrDict[\b] = \backswing;	
		
		instrDict[\C] = \camclick;	
		instrDict[\c] = \camera;	
		
		instrDict[\D] = \dentist;	
		instrDict[\d] = \drrr;		
		
		instrDict[\E] = \electric;	
		instrDict[\e] = \elbom;	
		
		instrDict[\F] = \fuzz;
		instrDict[\f] = \flash;	

		instrDict[\G] = \harshi;	
		instrDict[\g] = \glitch;
		
		instrDict[\H] = \harshlow;	
		instrDict[\h] = \machine;
		
		instrDict[\I] = \irritia;	
		instrDict[\i] = \lolo;	

		instrDict[\J] = \magnet;
		instrDict[\j] = \motorclick;
	
		instrDict[\K] = \midcrash;	
		instrDict[\k] = \cling;	

		instrDict[\L] = \magnetclock;	
		instrDict[\l] = \drrr;	
		
		instrDict[\M] = \machine;	
		instrDict[\m] = \nervous;	

		instrDict[\N] = \nasty;	
		instrDict[\n] = \noise;		
		
		instrDict[\O] = \phoo;
		instrDict[\o] = \olol;
		
		instrDict[\P] = \razor;	
		instrDict[\p] = \rattling;	
		
		instrDict[\Q] = \roughnoise;	
		instrDict[\q] = \robbie;	

		instrDict[\R] = \robotnoise;
		instrDict[\r] = \robohick;
			
		instrDict[\S] = \rought;	
		instrDict[\s] = \rooph;	
		
		instrDict[\T] = \servomotor;	
		instrDict[\t] = \servo;
		
		instrDict[\U] = \softmachine;
		instrDict[\u] = \sparkstatic;	
		
		instrDict[\V] = \softnoise;	
		instrDict[\v] = \speed;	

		instrDict[\W] = \sparkup;	
		instrDict[\w] = \sweetmachine;	
		
		instrDict[\X] = \static;	
		instrDict[\x] = \throat;
		
		instrDict[\Y] = \statics;	
		instrDict[\y] = \wooo;	
		
		instrDict[\Z] = \viromachine;	
		instrDict[\z] = \yoyo;	
		
		instrDict.writeArchive("sounds/ixilang/"++project++"/keyMapping.ixi");

	*/
