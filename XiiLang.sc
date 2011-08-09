
/*

// XiiLang.new("project", "key") - "project" stands for the name of the folder where soundfiles are kept. in this folder a _keyMapping.ixi file can be found that maps letters to sounds. if there is no file, then the mapping will be random

TODO: Add parameter effect control
TODO: Check the use of String:drop(1) and String:drop(-1)


*/

// problem is that method is also parsed at in parseMethod (not only in operator)

// new in ixi lang 3
// midiout
// midiclients
// languages (you can write in your native language, or create your own language for ixi lang)
// fractional time multiplication
// snapshots store and register effect states
// netclock?
// matrix
// coder (keys)
// save and load sessions (need to put into help file)
// suicide hotline added (you can now change your mind, in case performance has picked up)
// - automatic code writing (autocoder 4)


// FIXED BUG: snapshots do not perk up agents that have been dozed
// FIXED BUG: Snapshots do not recall the effect state
// FIXED BUG: single character percussive mode agents wont >shift
// FIXED BUG: future bug

// Add: ptpd support for netclocks
// Êsudo ./ptpd -cd
// Êhttp://sourceforge.net/projects/ptpd/develop

// post ICMC ideas:
// - Live recording into sound buffer by pressing shift+key (but save as livekey_a.aif and redefine SynthDef)
// - find agent by comparing strings
// - netclock
// - morphing


XiiLang {	
	classvar globaldocnum;
	var <>doc, docnum, oncolor, offcolor, processcolor, proxyspace, groups;
	var agentDict, instrDict, ixiInstr, effectDict, chordDict, snapshotDict; //, effectRegDict; //, codeDict;
	var scale, tuning, chosenscale, tonic;
	var midiclient, eventtype, suicidefork;
	var langCommands, englishCommands, language, english;
	var matrixArray, initargs; // matrix vars
	var projectname, key;
	
	*new { arg project="default", keyarg="C", txt=false, newdoc=false, language, dicts; // current doc and the project to use (folder of soundfiles)
		^super.new.initXiiLang( project, keyarg, txt, newdoc, language, dicts );
	}
		
	initXiiLang {arg project, keyarg, txt, newdoc, lang, dicts;
		if(globaldocnum.isNil, {
			globaldocnum = 0;
		}, {
			globaldocnum = globaldocnum+1;
		});
		key = keyarg;
		initargs = [project, key, txt, newdoc, lang];
		XiiLangSingleton.new(this, globaldocnum);
		// the number of this document (allows for multiple docs using same variable names)
		docnum = globaldocnum; // this number is now added in front of all agent names
		chosenscale = Scale.major; // this is scale representation
		scale = chosenscale.degrees.add(12); // this is in degrees
		tuning = \et12; // default tuning
		tonic = 60 + [\C, \Cs, \D, \Ds, \E, \F, \Fs, \G, \Gs, \A, \As, \B].indexOf(key.toUpper.asSymbol); // midinote 60 is the default
		oncolor = Color.white;
		offcolor = Color.green;
		processcolor = Color.yellow;
		if(dicts.isNil, {
			agentDict = IdentityDictionary.new; // dicts are sent from "load"
			chordDict = IdentityDictionary.new;
			snapshotDict = IdentityDictionary.new; // dicts are sent from "load"
			groups = ();
		},{
			agentDict = dicts[0]; // dicts are sent from "load"
			chordDict = IdentityDictionary.new;
			snapshotDict = dicts[1]; // dicts are sent from "load"
			groups = dicts[2];
			docnum = dicts[3];
		});
		TempoClock.default.tempo = 120/60;
		projectname = project;
		
		midiclient = 0;
		
		try{ // try necessary, since a user without MIDI busses reported that the next two lines gave errors
			MIDIClient.init;
			// midiclient = MIDIOut(0, MIDIClient.destinations[0].uid);
			midiclient = MIDIOut(0);
		};
		
		eventtype = \note;
		this.makeEffectDict; // in a special method, as it has to be called after every cmd+dot
		this.envirSetup( txt, newdoc, project );
	\deb0.postln;
		ixiInstr = XiiLangInstr.new(project);
		instrDict = ixiInstr.makeInstrDict;
		proxyspace = ProxySpace.new.know_(true);
	\deb1.postln;

		CmdPeriod.add({
			16.do({arg i; try{midiclient.allNotesOff(i)} });
			this.makeEffectDict;
			groups = ();
			agentDict = IdentityDictionary.new;
			chordDict = IdentityDictionary.new;
			snapshotDict = IdentityDictionary.new;
			proxyspace = ProxySpace.new.know_(true);
		});
		englishCommands = ["group", "sequence", "future", "snapshot", "->", "))", "((", "|", "[", "{", "(", ".", ">>", "<<", "tempo", 
				"scale", "scalepush", "tuning", "tuningpush", "remind", "help", "tonality", "instr", "tonic", "grid", "kill",  
				"doze", "perk", "nap", "shake", "swap", ">shift", "<shift", "invert", "expand", "revert", 
				"up", "down", "yoyo", "order", "suicide", "hotline", "dict", "save", "load", "midiclients", 
				"midiout", "matrix", "autocoder", "coder"];
		
		if(lang.isNil, { 
			"lang is nil".postln;
			english = true; // might not need this;
			langCommands = englishCommands;
		}, {
			english = false;
			language = XiiLangDicts.getDict(lang.asSymbol);
			langCommands = XiiLangDicts.getList(lang.asSymbol);
		});
	}

	makeEffectDict { // more to come here + parameter control - for your own effects, simply add a new line to here and it will work out of the box
		effectDict = IdentityDictionary.new;
		effectDict[\reverb] 	= {arg sig; (sig*0.6)+FreeVerb.ar(sig, 0.85, 0.86, 0.3)};
		effectDict[\reverbL] 	= {arg sig; (sig*0.6)+FreeVerb.ar(sig, 0.95, 0.96, 0.7)};
		effectDict[\reverbS] 	= {arg sig; (sig*0.6)+FreeVerb.ar(sig, 0.45, 0.46, 0.2)};
		effectDict[\delay]  	= {arg sig; sig + AllpassC.ar(sig, 1, 0.15, 1.3 )};
		effectDict[\lowpass] 	= {arg sig; RLPF.ar(sig, 1000, 0.2)};
		effectDict[\tremolo]	= {arg sig; (sig * SinOsc.ar(2.1, 0, 5.44, 0))*0.5};
		effectDict[\vibrato]	= {arg sig; PitchShift.ar(sig, 0.008, SinOsc.ar(2.1, 0, 0.11, 1))};
		effectDict[\techno] 	= {arg sig; RLPF.ar(sig, SinOsc.ar(0.1).exprange(880,12000), 0.2)};
		effectDict[\technosaw] 	= {arg sig; RLPF.ar(sig, LFSaw.ar(0.2).exprange(880,12000), 0.2)};
		effectDict[\distort] 	= {arg sig; (3111.33*sig.distort/(1+(2231.23*sig.abs))).distort*0.02};
		effectDict[\cyberpunk]	= {arg sig; Squiz.ar(sig, 4.5, 5, 0.1)};
		effectDict[\bitcrush]	= {arg sig; Latch.ar(sig, Impulse.ar(11000*0.5)).round(0.5 ** 6.7)};
		effectDict[\antique]	= {arg sig; LPF.ar(sig, 1700) + Dust.ar(7, 0.6)};
	}

	//set up document and the keydown control
	envirSetup { arg txt, newdoc, project;
		if(newdoc, {
			doc = Document.new;		
		}, {
			doc = Document.current;
		});
		doc.bounds_(Rect(400,400, 1000, 600));
		doc.background_(Color.black);
		doc.stringColor_(oncolor);
		if(txt == false, { doc.string_("") });
		doc.name_("ixi lang   -   project :" + project.quote + "  -   window nr:" + docnum.asString);
		doc.font_(Font("Monaco",20));
		doc.promptToSave_(false);		
		doc.keyDownAction_({|doc, char, mod, unicode, keycode | 
			var linenr, string;
			// evaluating code (the next line will use .isAlt, when that is available 
			if((mod & 524288 == 524288) && ((keycode==124)||(keycode==123)||(keycode==125)||(keycode==126)), { // alt + left or up or right or down arrow keys
				linenr = doc.string[..doc.selectionStart-1].split($\n).size;
				doc.selectLine(linenr);
				string = doc.selectedString;
				if(keycode==123, { // not 124, 125, 
					this.freeAgent(string);
				}, {
					this.opInterpreter(string);	
				});				
			});		
		});
		doc.onClose_({
			doc.background_(Color.white); // for some reason this is not saved when the doc is closed/saved
			doc.stringColor_(Color.black);	
			proxyspace.end(4);  // free all proxies
			agentDict.do({arg agent;
				agent[2].stop;
				agent[2] = nil;
			});
			snapshotDict[\futures].stop; 
		});
	}
	
	// the interpreter of thie ixi lang - here operators are overwritten
	opInterpreter {arg string;
		var oldop, operator; // the various operators of the language
		var methodFound = false;
		string = string.reject({ |c| c.ascii == 10 }); // get rid of char return XXX TESTING (before Helsinki GIG) 
		operator = block{|break| // better NOT mess with the order of the following... (operators using -> need to be before "->")
			langCommands.do({arg op; var suggestedop, space;
				var c = string.find(op);
				//[\op, op].postln;
				if(c.isNil.not, {
					space = string[c..string.size].find(" "); // allowing for longer operators with same beginning as shorter
					if(space.isNil.not, { 
						suggestedop = string[c..(c+(space-1))];
					},{	
						suggestedop = op;
					});
					if(suggestedop.size == op.size, { // this is a bit silly (longer op always has to be after the short)
						methodFound = true;
						break.value(op);
					});
				}); 
			});
			if(methodFound == false, {" --->   ixi lang error : OPERATOR NOT FOUND !!!".postln; });
		};
		
		if(english.not, { // this is the only place of non-english code apart from in the init function
			oldop = operator;
			operator = try{ language[oldop.asSymbol] } ? operator; // if operator exists in the lang dict, else use the given
			operator = operator.asString;
			string = string.replace(oldop, operator);
		});

		switch(operator)
			{"dict"}{ // TEMP: only used for debugging
				"-- Groups : ".postln;
				Post << groups; "\n".postln;
				
				"-- agentDicts : ".postln;
				Post << agentDict; "\n".postln;
				
				"-- snapshotDicts : ".postln;
				Post << snapshotDict; "\n".postln;
			}
			{"save"}{ 
				var sessionstart, sessionend, session;
				string = string.replace("    ", " ");
				string = string.replace("   ", " ");
				string = string.replace("  ", " ");
				string = string++" "; // add a space in order to find end of agent (if no argument)
				sessionstart = string.findAll(" ")[0];
				sessionend = string.findAll(" ")[1]; 
				session = string[sessionstart+1..sessionend-1];
				[projectname, key, language, doc.string, agentDict, snapshotDict, groups, docnum].writeArchive("ixilang/"++session++".ils");
			}
			{"load"}{
				var sessionstart, sessionend, session, key, language, projectname;
				var tempAgentDict, tempSnapshotDict;
				string = string.replace("    ", " ");
				string = string.replace("   ", " ");
				string = string.replace("  ", " ");
				string = string++" "; // add a space in order to find end of agent (if no argument)
				sessionstart = string.findAll(" ")[0];
				sessionend = string.findAll(" ")[1]; 
				session = string[sessionstart+1..sessionend-1];
				doc.onClose; // call this to end all agents and groups in this particular doc if it has been running agents
				#projectname, key, language, string, agentDict, snapshotDict, groups, docnum = Object.readArchive("ixilang/"++session++".ils");
				doc.string = string;
				XiiLang.new( projectname, key, true, false, language, [agentDict, snapshotDict, groups, docnum]);
			}
			{"snapshot"}{
				this.parseSnapshot(string);
			}
			{"->"}{
				var mode;
				mode = block{|break| 
					["|", "[", "{", "("].do({arg op, i;						var c = string.find(op);
						if(c.isNil.not, {break.value(i)}); 
					});
				};
				switch(mode)
					{0} { this.parseScoreMode0(string) }
					{1} { this.parseScoreMode1(string) }
					{2} { this.parseScoreMode2(string) }
					{3} { this.parseChord(string) };
			}
			{"|"}{ // the following four are here in case user is not using agent assignment (->)
				this.parseScoreMode0(string);
			}
			{"["}{
				this.parseScoreMode1(string);
			}
			{"{"}{
				this.parseScoreMode2(string);
			}
			{"("}{
				this.parseChord(string);
			}
			{"future"}{
				// future 8:4 >> swap thor // every 8 seconds the action is performed (here 4 times)
				// future << thor // cancel the scheduling
				var command, commandstart, colon, seconds, times, agent, pureagent, agentstart, agentend;
				var cursorPos, testcond, snapshot;
				string = string.reject({ |c| c.ascii == 10 }); // get rid of char return
				// allow for some sloppyness in style
				string = string.replace("    ", " ");
				string = string.replace("   ", " ");
				string = string.replace("  ", " ");
				string = string++" "; // add a space in order to find end of agent (if no argument)

				if(string.contains(">>"), { // setting future event(s)
					commandstart = string.find(">");
					colon = string.find(":");
					seconds = string[6..colon-1].asInteger;
					times = string[colon+1..commandstart-1].asInteger;

					if(string[commandstart+3..commandstart+10] == "snapshot", { // it's the "choose snapshot" future
						snapshotDict[\futures].stop;
						snapshotDict[\futures] = // don't need to store the name, just a unique name
							{ 
								times.do({arg i; 
									seconds.wait;
									testcond = false;
									while({ testcond.not }, {
										snapshot = snapshotDict.keys.choose;
										if(snapshot != \futures, { testcond = true });
									});
									{ 
									cursorPos = doc.selectionStart; // get cursor pos
									this.parseSnapshot("snapshot " ++ snapshot.asString);
									doc.selectRange(cursorPos); // set cursor pos again
									}.defer;
								}) 
							}.fork(TempoClock.new) // not using default clock, since new tempo affects wait (strange?/bug?)
					}, {
						agentstart = string.findAll(" ")[3];
						agentend = string.findAll(" ")[4]; 
						pureagent = string[agentstart+1..agentend-1];
						agent = (docnum.asString++pureagent).asSymbol;
						command = string[commandstart+3..agentstart-1];
						
						if(groups.at(agent).isNil.not, { // the "agent" is a group so we need to set routine to each of the agents in the group
							groups.at(agent).do({arg agentx, i;
								agentx.postln;
								agent = (docnum.asString++agentx).asSymbol;
								if(agentDict[agent][2].isNil, {
									{var thisAgent; // this is needed so next agent doesn't overwrite the earlier one
									thisAgent = agentDict[agent][2]; 
									thisAgent =
										{ 
											times.do({arg i; 
												seconds.wait;
												{ 
												cursorPos = doc.selectionStart; // get cursor pos
												this.parseMethod(command+agentx); // do command
												doc.selectRange(cursorPos); // set cursor pos again
												}.defer;
											});
											thisAgent = nil; // set it back to nil after routine is finished
										}.fork(TempoClock.new)
									}.value;
								});
							});
						}, { // it is a real agent, not a group, then we ADD THE EFFECT
							if(agentDict[agent][2].isNil, {
								agentDict[agent][2] = 
									{ 
										times.do({arg i; 
											seconds.wait;
											{ 
											cursorPos = doc.selectionStart; // get cursor pos
											this.parseMethod(command+pureagent); // do command
											doc.selectRange(cursorPos); // set cursor pos again
											}.defer;
										});
										agentDict[agent][2] = nil; // set it back to nil after routine is finished
									}.fork(TempoClock.new)
							});
						});
					});
				}, { // removing future scheduling
					agentstart = string.findAll(" ")[1];
					agentend = string.findAll(" ")[2]; 
					agent = string[agentstart+1..agentend-1];
					agent = (docnum.asString++agent).asSymbol;

					if(groups.at(agent).isNil.not, { // the "agent" is a group so we need to set routine to each of the agents in the group
						groups.at(agent).do({arg agentx, i;
							agent = (docnum.asString++agentx).asSymbol;
							agentDict[agent][2].stop;
							agentDict[agent][2] = nil;
						});
					},{
						agentDict[agent][2].stop;
						agentDict[agent][2] = nil;
					});
				});
			}
			{">>"}{
				this.initEffect(string);
			}
			{"<<"}{
				this.removeEffect(string);
			}
			{"))"}{
				this.increaseAmp(string);
			}
			{"(("}{
				this.decreaseAmp(string);
			}
			{"tempo"}{
				var newtemp, time, op;
				var nrstart = string.find("o")+1;
				if(string.contains(":"), {
					string = string.tr($ , \);
					op = string.find(":");
					newtemp = string[nrstart..op].asInteger/60;
					time = string[op+1..string.size-1].asInteger;
					TempoClock.default.sync(newtemp, time);
					try{ matrixArray.do({arg matrix; matrix.setSyncTempo_(newtemp, time) }) };
				}, {
					newtemp = string[nrstart+1..string.size-1].asInteger / 60;
					TempoClock.default.tempo = newtemp;
					try {matrixArray.do({arg matrix; matrix.setTempo_(newtemp) }) };
				});
			}
			{"scale"}{
				var scalestart, scalestr;
				scalestart = string.find("e");
				scalestr = string.tr($ , \);
				scalestr = scalestr[scalestart+1..scalestr.size-1];
				chosenscale = ("Scale."++scalestr).interpret;
				chosenscale.tuning_(tuning.asSymbol);
				scale = chosenscale.semitones.add(12); // used to be degrees, but that doesn't support tuning
			}
			{"scalepush"}{
				var scalestart, scalestr;
				var dictscore, mode;
				scalestart = string.find("h");
				scalestr = string.tr($ , \);
				scalestr = scalestr[scalestart+1..scalestr.size-1];
				chosenscale = ("Scale."++scalestr).interpret;
				chosenscale.tuning_(tuning.asSymbol);
				scale = chosenscale.semitones.add(12); // used to be degrees, but that doesn't support tuning
				agentDict.do({arg agent;
					dictscore = agent[1].scorestring;
					dictscore = dictscore.reject({ |c| c.ascii == 34 }); // get rid of quotation marks
					mode = block{|break| 
						["|", "[", "{", "("].do({arg op, i;						var c = dictscore.find(op);
							if(c.isNil.not, {break.value(i)}); 
						});
					};
					switch(mode)
						{0} { this.parseScoreMode0(dictscore) }
						{1} { this.parseScoreMode1(dictscore) }
						{2} { this.parseScoreMode2(dictscore) };
					if(agent[1].playstate == true, {
						proxyspace[agent].play;
					});
				});
			}
			{"tuning"}{
				var tuningstart, tuningstr;
				tuningstart = string.find("g");
				tuningstr = string.tr($ , \);
				tuning = tuningstr[tuningstart+1..tuningstr.size-1];
				tuning = tuning.reject({ |c| c.ascii == 10 }); // get rid of char return
				chosenscale.tuning_(tuning.asSymbol);
				scale = chosenscale.semitones.add(12);
			}
			{"tuningpush"}{
				var tuningstart, tuningstr;
				var dictscore, mode;
				tuningstart = string.find("g");
				tuningstr = string.tr($ , \);
				tuning = tuningstr[tuningstart+1..tuningstr.size-1];
				tuning = tuning.reject({ |c| c.ascii == 10 }); // get rid of char return
				chosenscale.tuning_(tuning.asSymbol);
				scale = chosenscale.semitones.add(12);
				agentDict.do({arg agent;
					dictscore = agent[1].scorestring;
					dictscore = dictscore.reject({ |c| c.ascii == 34 }); // get rid of quotation marks
					mode = block{|break| 
						["|", "[", "{", "("].do({arg op, i;						var c = dictscore.find(op);
							if(c.isNil.not, {break.value(i)}); 
						});
					};
					switch(mode)
						{0} { this.parseScoreMode0(dictscore) }
						{1} { this.parseScoreMode1(dictscore) }
						{2} { this.parseScoreMode2(dictscore) };
					if(agent[1].playstate == true, {
						proxyspace[agent].play;
					});
				});
			}
			{"remind"}{
				this.getMethodsList;		
			}
			{"help"}{
				this.getMethodsList;		
			}
			{"tonality"}{
				var doc;
				doc = Document.new;
				doc.name_("scales");
				doc.promptToSave_(false);
				doc.background_(Color.black);
				doc.stringColor_(Color.green);
				doc.bounds_(Rect(10, 500, 500, 800));
				doc.font_(Font("Monaco",16));
				doc.string_("Scales: " + ScaleInfo.scales.keys.asArray.sort.asCompileString
				+"\n\n Tunings: "+
				"et12, pythagorean, just, sept1, sept2, mean4, mean5, mean6, kirnberger, werkmeister, vallotti, young, reinhard, wcHarm, wcSJ");
			}
			{"instr"}{
				this.getInstrumentsList;
			}
			{"tonic"}{
				var tonicstart, tstring, tonicstr;
				tonicstart = string.find("c");
				tstring = string.tr($ , \);
				tonicstr = tstring[tonicstart+1..tstring.size-1];
				tonicstr = tonicstr.reject({ |c| c.ascii == 10 }); // get rid of char return
				tonic = tonicstr.asInteger;
			}
			{"grid"}{
				var cursorPos, gridstring, meter, grids, gridstart;
				cursorPos = doc.selectionStart; // get cursor pos
				meter = string[string.find(" ")..string.size-1].asInteger;
				if(meter == "grid", {meter = 1});
				gridstring = "";
				50.do({arg i; gridstring = gridstring++if((i%meter)==0, {"|"}, {" "})  });
				doc.string_(doc.string.replace(string, gridstring++"\n"));
			}
			{"kill"}{
				proxyspace.end;
				proxyspace = ProxySpace.new.know_(true);
				agentDict.do({arg agent;
					agent[2].stop;
					agent[2] = nil;
				});
				snapshotDict[\futures].stop;
			}
			{"group"}{
				var spaces, groupname, op, groupitems;
				" --->   ixi lang : MAKING A GROUP : ".post;
				// allow for some sloppyness in style
				string = string.replace("    ", " ");
				string = string.replace("   ", " ");
				string = string.replace("  ", " ");
				string = string++" "; // add an extra space at the end
				string = string.reject({ |c| c.ascii == 10 }); // get rid of char return
				spaces = string.findAll(" ");
				groupname = string[spaces[0]+1..spaces[1]-1];
				groupname = (docnum.asString++groupname).asSymbol; // multidoc support
				op = string.find("->");
				groupitems = [];
				(spaces.size-1).do({arg i; 
					if(spaces[i] > op, { groupitems = groupitems.add( string[spaces[i]+1..spaces[i+1]-1].asSymbol ) }) 
				});
				groups.add(groupname -> groupitems);
				groups.postln;
			}
			{"sequence"}{
				var spaces, sequenceagent, op, seqagents, typecheck, firsttype, sa, originalstring, originalagents, fullscore;
				var notearr, durarr, sustainarr, instrarr, amparr, score, instrument, quantphase, newInstrFlag = false;
				typecheck = 0;
				notearr = [];
				durarr = [];
				sustainarr = [];
				amparr = [];
				instrarr = [];
				score = "";
				originalstring = string;
				" --->   ixi lang : MAKING A SEQUENCE : ".postln;
				// allow for some sloppyness in style
				string = string.replace("    ", " ");
				string = string.replace("   ", " ");
				string = string.replace("  ", " ");
				string = string++" "; // add an extra space at the end
				string = string.reject({ |c| c.ascii == 10 }); // get rid of char return
				spaces = string.findAll(" ");
				sequenceagent = string[spaces[0]+1..spaces[1]-1];
				sa = sequenceagent;
				sequenceagent = (docnum.asString++sequenceagent).asSymbol; // multidoc support
				op = string.find("->");
				seqagents = [];
				originalagents = [];				
				(spaces.size-1).do({arg i; 
					if(spaces[i] > op, { 
						seqagents = seqagents.add((docnum.asString++string[spaces[i]+1..spaces[i+1]-1]).asSymbol);
						originalagents = originalagents.add(string[spaces[i]+1..spaces[i+1]-1]);
					});
				});
				// check if all items are of same type
				firsttype = agentDict[seqagents[0]][1].mode;
				seqagents.do({arg agent; typecheck = typecheck + agentDict[agent][1].mode; });
				
				// then merge their score into one string (see below the dict idea)
				if((typecheck/seqagents.size) != firsttype, {
					" --->   ixi lang: ERROR! You are trying to mix playmodes".postln;
				}, {
					switch(firsttype)
						{0} {
							seqagents.do({arg agent, i;
								durarr = durarr ++ agentDict[agent][1].durarr;
								sustainarr = sustainarr ++ agentDict[agent][1].sustainarr;
								instrarr = instrarr ++ agentDict[agent][1].instrarr;
								score = score ++ agentDict[agent][1].score; // just for creating the score in the doc
							});
							fullscore = (sa++" -> |"++score++"|");
							if(agentDict[sequenceagent].isNil, {
								agentDict[sequenceagent] = [ (), ().add(\amp->0.3), nil];
							}, {
								if(agentDict[sequenceagent][1].scorestring.contains("{"), {newInstrFlag = true }); // free if { instr (Pmono is always on)
							}); // 1st = effectRegistryDict, 2nd = scoreInfoDict, 3rd = placeholder for a routine
								quantphase = agentDict[seqagents[0]][1].quantphase;
								agentDict[sequenceagent][1].scorestring = fullscore.asCompileString;
								agentDict[sequenceagent][1].instrument = "rhythmtrack";
								agentDict[sequenceagent][1].durarr = durarr;
								agentDict[sequenceagent][1].sustainarr = sustainarr;
								agentDict[sequenceagent][1].instrarr = instrarr;
								agentDict[sequenceagent][1].score = score;
								agentDict[sequenceagent][1].quantphase = quantphase;
								doc.string_(doc.string.replace(originalstring, fullscore++"\n"));
								this.playScoreMode0(sequenceagent, durarr, instrarr, sustainarr, quantphase, newInstrFlag);
						}
						{1} {
							seqagents.do({arg agent, i;
								durarr = durarr ++ agentDict[agent][1].durarr;
								sustainarr = sustainarr ++ agentDict[agent][1].sustainarr;
								notearr = notearr ++ agentDict[agent][1].notearr;
								score = score ++ agentDict[agent][1].score; // just for creating the score in the doc
							});
							quantphase = agentDict[seqagents[0]][1].quantphase;
							instrument = agentDict[seqagents[0]][1].instrument;
							fullscore = (sa++" -> "++ instrument ++"["++score++"]");
							if(agentDict[sequenceagent].isNil, {
								agentDict[sequenceagent] = [ (), ().add(\amp->0.3), nil];
							}, {
								if(agentDict[sequenceagent][1].scorestring.contains("{"), {newInstrFlag = true }); // free if { instr (Pmono is always on)
							}); // 1st = effectRegistryDict, 2nd = scoreInfoDict, 3rd = placeholder for a routine

								agentDict[sequenceagent][1].scorestring = fullscore.asCompileString;
								agentDict[sequenceagent][1].instrument = instrument;
								agentDict[sequenceagent][1].durarr = durarr;
								agentDict[sequenceagent][1].sustainarr = sustainarr;
								agentDict[sequenceagent][1].notearr = notearr;
								agentDict[sequenceagent][1].score = score;
								agentDict[sequenceagent][1].quantphase = quantphase;
								doc.string_(doc.string.replace(originalstring, fullscore++"\n"));
								this.playScoreMode1(sequenceagent, notearr, durarr, sustainarr, instrument, quantphase, newInstrFlag);
						}
						{2} {
							seqagents.do({arg agent, i;
								durarr = durarr ++ agentDict[agent][1].durarr;
								amparr = notearr ++ agentDict[agent][1].amparr;
								score = score ++ agentDict[agent][1].score; // just for creating the score in the doc
							});
							quantphase = agentDict[seqagents[0]][1].quantphase;
							instrument = agentDict[seqagents[0]][1].instrument;
							fullscore = (sa++" -> "++ instrument ++"{"++score++"}");
							if(agentDict[sequenceagent].isNil, {
								" creating a new Dict !!!!!!!!".postln;
								agentDict[sequenceagent] = [ (), ().add(\amp->0.3), nil];
							}, {
								if(agentDict[sequenceagent][1].scorestring.contains("{"), {newInstrFlag = true }); // free if { instr (Pmono is always on)
							}); // 1st = effectRegistryDict, 2nd = scoreInfoDict, 3rd = placeholder for a routine
								agentDict[sequenceagent][1].scorestring = fullscore.asCompileString;
								agentDict[sequenceagent][1].instrument = instrument;
								agentDict[sequenceagent][1].durarr = durarr;
								agentDict[sequenceagent][1].amparr = amparr;
								agentDict[sequenceagent][1].score = score;
								agentDict[sequenceagent][1].quantphase = quantphase;
								doc.string_(doc.string.replace(originalstring, fullscore++"\n"));
								this.playScoreMode2(sequenceagent, amparr, durarr, instrument, quantphase, newInstrFlag);
						};
				});				
			}

			// methods (called verbs in ixi lang) dealt with in the parsMethod below (these change string in doc)
			{"doze"} {
				this.parseMethod(string);
			} 
			{"perk"}{
				this.parseMethod(string);			
			}
			{"nap"}{
				this.parseMethod(string);			
			}
			{"shake"}{
				this.parseMethod(string);
			}
			{"swap"}{
				this.parseMethod(string);
			}
			{">shift"}{
				this.parseMethod(string);
			}
			{"<shift"}{
				this.parseMethod(string);
			}
			{"invert"}{
				this.parseMethod(string);
			}
			{"expand"}{
				this.parseMethod(string);
			}
			{"revert"}{
				this.parseMethod(string);
			} 
			{"up"}{
				this.parseMethod(string);
			}
			{"down"}{
				this.parseMethod(string);
			}
			{"yoyo"}{
				this.parseMethod(string);
			}
			{"order"}{
				this.parseMethod(string);			
			}
			{"suicide"}{
				var chance, time, op;
				var nrstart = string.find("e")+1;
				if(string.contains(":"), {
					string = string.tr($ , \);
					op = string.find(":");
					chance = string[nrstart..op].asInteger;
					time = string[op+1..string.size-1].asInteger;
					suicidefork = fork{
						inf.do({
						["---  WILL I DIE NOW? ---", "---  HAS IT COME TO AN END?  ---", "---  WHEN WILL I DIE?  ---", "---  MORRISSEY  ---", "---  ACTUALLY, WAIT A MINUTE...  ---", "---  I'VE HAD IT  ---", "---  THIS IS THE END  ---", "---  I'M PATHETIC  ---"].choose.postln;
						if((chance/100).coin, { 0.exit }); // kill supercollider
						time.wait;
						});
					};
				});
			}
			{"hotline"}{
				suicidefork.stop;
			}
			{"midiclients"}{
				"-----------   Avalable midiclients   ------------ ".postln;
				MIDIClient.destinations.do({ |x, i| Post << "device " << i << Char.tab << x << Char.nl });
			}
			{"midiout"}{
				var nrstart, destination;
				nrstart = string.find("t")+1;
				destination = string[nrstart..string.size-1].asInteger;
				midiclient = MIDIOut(destination);
			}
			{"matrix"}{
				var spaces, size=8, direction=\x;
				spaces = string.findAll(" ");
				if(spaces.size > 0, { size = string[spaces[0]..spaces[1]].asInteger; });
				if(spaces.size > 1, {
					direction = string[spaces[1]+1..string.size-1].tr($ , \).asSymbol;
				});
				matrixArray = matrixArray.add( XiiLangMatrix.new(size, direction, instrDict) );
			}
			{"coder"}{
				var xiilang, tempstring, scorestring, coderarraysum, quantspaces, quant;

				xiilang = XiiLang.new(initargs[0], initargs[1], initargs[2], true, initargs[4]);
				xiilang.doc.name_("ixi lang coder");
				xiilang.doc.keyDownAction_({| thisdoc, char, mod, unicode, keycode | 
					var linenr, string;
					if((mod & 524288 == 524288) && ((keycode==124)||(keycode==123)||(keycode==125)||(keycode==126)), {
						linenr = thisdoc.string[..thisdoc.selectionStart-1].split($\n).size;
						thisdoc.selectLine(linenr);
						string = thisdoc.selectedString;
						if(keycode==123, { // not 124, 125, 
							xiilang.freeAgent(string);
						}, {
							xiilang.opInterpreter(string);
						});				
					});
					// here adding what the coder is about 
					if(char.isAlpha, {
						Synth(instrDict[char.asSymbol], [\freq, 60.midicps]); // original
					});
				});
			}			
			{"autocoder"}{
				var agent, mode, instrument, score, line, charloc, cursorPos, density, lines, linenr;
				charloc = doc.selectionStart+string.size;
				{doc.insertTextRange("\n", charloc, 0)}.defer; 
				lines = "";
				string.collect({arg char; if(char.isDecDigit, {lines = lines++char}) });
				lines = lines.asInteger;
				{	
					lines.do({
						mode = [0,1,2].wchoose([0.5, 0.35, 0.15]);
						agent = "abcdefghijklmnopqrstuvxyzaeyiuxz".scramble[0..(2+(4.rand))];
						density = rrand(0.15, 0.45);
						switch(mode)
							{0}{
								score = "";
								[16, 20, 24].choose.do({arg char;
									score = score ++ if(density.coin, {
										"abcdefghijklmnopqrstuvxABCDEFGHIJKLMNOPQRSTUVXYZ".scramble.choose
									}, {" "});
								});
								score = "|"++score++"|";
								line = agent+"->"+score+"\n";
								line.postln;
							}
							{1}{
								instrument = (ixiInstr.returnMelodicInstr++
								ixiInstr.returnMelodicInstr++ixiInstr.returnMelodicInstr++
								ixiInstr.returnMelodicInstr++ixiInstr.returnPercussiveInstr).choose;
								score = "";
								[16, 20, 24].choose.do({
									score = score ++ if(density.coin, {10.rand}, {" "});
								});
								score = instrument++"["++score++"]";
								line = agent+"->"+score+"\n";
								line.postln;
							}
							{2}{
								instrument = ixiInstr.returnPercussiveInstr.choose;
								score = "";
								[16, 20, 24].choose.do({
									score = score ++ if(density.coin, {10.rand}, {" "});
								});
								score = instrument++"{"++score++"}";
								line = agent+"->"+score+"\n";
								line.postln;
							};
						line.do({arg char, i; 
							charloc = charloc + 1; 
							{doc.insertTextRange(char.asString, charloc, 0)}.defer; 
							[0.1, 0.05, 0.5].wchoose([0.5, 0.4, 0.1]).wait;
						});
						1.wait;
						this.opInterpreter(line);
						"sending this to interpreter:-".post; line.post; "-".postln;
					});
				}.fork(TempoClock.new);
			}
			;
		}
	
	// method invoked on alt+left arrow, for easy freeing of an agent (line)
	freeAgent { arg string;
		var prestring, splitloc, agent, linenr;
		linenr = doc.string[..doc.selectionStart-1].split($\n).size;
		doc.selectLine(linenr);
		string = string.tr($ , \);
		splitloc = string.find("->");
		agent = string[0..splitloc-1]; // get the name of the agent
		agent = (docnum.asString++agent).asSymbol;
		proxyspace[agent].clear;
		agentDict[agent] = nil;
		{doc.stringColor_(Color.red, doc.selectionStart, doc.selectionSize)}.defer(0.1); // killed code is red
	}
	
	parseSnapshot{ arg string; // XXX
		var snapshotname, splitloc, agentDICT;
		string = string.reject({ |c| c.ascii == 10 }); // get rid of char return

		if(string.contains("->"), { // STORE A SNAPSHOT
			" --->   ixi lang: storing a snapshot".postln;
			string = string.tr($ , \);
			splitloc = string.find(">");
			snapshotname = string[splitloc+1 .. string.size-1];
			snapshotDict[snapshotname.asSymbol] = nil; // remove properly;
			snapshotDict[snapshotname.asSymbol] = agentDict.deepCopy;
		}, {	// RECALL A SNAPSHOT
			" --->   ixi lang: recalling a snapshot".postln;
			splitloc = string.find(" ");
			snapshotname = string[splitloc+1 .. string.size-1];
			agentDICT = snapshotDict[snapshotname.asSymbol]; // a local variable of a snapshot agent dictionary (not the global one)
			
			// stop all agents that are not in the snapshot
			agentDict.keys.do({arg agent;
				var allreturns, stringstart, stringend, pureagentname;
				pureagentname = agent.asString;
				pureagentname = pureagentname[1..pureagentname.size-1];

				if(agentDICT[agent].isNil, { 
					proxyspace[agent].stop; // xxx
					// proxyspace[agent].objects[0].array[0].mute; // xxx
					allreturns = doc.string.findAll("\n");
					// the following checks if it's exactly the same agent name (and not confusing joe and joel)
					block{ | break |
					doc.string.findAll(pureagentname).do({arg loc, i;
						stringend = allreturns[allreturns.indexOfGreaterThan(loc)];
						if(doc.string[(loc+pureagentname.size)..stringend].contains("->"), {
							stringstart = loc; //doc.string.find(pureagentname);
							if(pureagentname == doc.string[stringstart..doc.string.findAll("->")[doc.string.findAll("->").indexOfGreaterThan(stringstart+1)]-1].tr($ , \), {
								break.value; //  exact match found and we break loop, leaving stringstart and stringend with correct values
							});
						});
					});
					};
					doc.stringColor_(offcolor, stringstart, stringend-stringstart);
				});
			});
			
			// then run the agents in the snapshot and replace strings in doc
			agentDICT.keysValuesDo({arg keyagentname, agentDictItem;
				var allreturns, stringstart, stringend, thisline, agentname, pureagentname, dictscore, mode, cursorPos;
				agentname = keyagentname.asString;
				pureagentname = agentname[1..agentname.size-1];

				// --  0)  Check if the agent is playing or not in that snapshot
				if(agentDictItem[1].playstate == true, {
					
					// --  1)  Find the agent in the doc
					allreturns = doc.string.findAll("\n");
					// the following checks if it's exactly the same agent name (and not confusing joe and joel)
					block{ | break |
					doc.string.findAll(pureagentname).do({arg loc, i;
						stringend = allreturns[allreturns.indexOfGreaterThan(loc)];
						if(doc.string[(loc+pureagentname.size)..stringend].contains("->"), {
							stringstart = loc; //doc.string.find(pureagentname);
							if(pureagentname == doc.string[stringstart..doc.string.findAll("->")[doc.string.findAll("->").indexOfGreaterThan(stringstart+1)]-1].tr($ , \), {
								break.value; //  exact match found and we break loop, leaving stringstart and stringend with correct values
							});
						});
					});
					};
					
					thisline = doc.string[stringstart..stringend];

					dictscore = agentDictItem[1].scorestring;
					dictscore = dictscore.reject({ |c| c.ascii == 34 }); // get rid of quotation marks

					// --  2)  Swap the string in the doc
					
					// either with the simple swap
					doc.string_( dictscore, stringstart, stringend-stringstart); // this one keeps text colour
					doc.stringColor_(oncolor, stringstart, stringend-stringstart);
						
					// --  3)  Run the code (parse it) - IF playstate is true
					
					mode = block{|break| 
						["|", "[", "{", "("].do({arg op, i;						var c = dictscore.find(op);
							if(c.isNil.not, {break.value(i)}); 
						});
					};
					switch(mode)
						{0} { this.parseScoreMode0(dictscore) }
						{1} { this.parseScoreMode1(dictscore) }
						{2} { this.parseScoreMode2(dictscore) };
					proxyspace[keyagentname].play;
					
					// --  4)  Set the effects that were active when the snapshot was taken
					
					10.do({arg i; proxyspace[keyagentname][i+1] =  nil }); // remove all effects (10 max) (+1 as 0 is Pdef)
					
					agentDICT[keyagentname][0].keys.do({arg key, i; 
						key.post; "  ".post; i.postln;
						proxyspace[keyagentname][i+1] = \filter -> effectDict[key.asSymbol];
					});

				});
			})
		});
	}

	parsePostfixArgs {arg postfixstring;
		var sustainstartloc, sustainendloc, sustainstring, sustainarr;
		var attacksymbols, attackstartloc, attackendloc, attackstring, attackarr;
		var argDict;
		
		argDict = ();
		argDict[\timestretch] = 1;
		argDict[\transposition] = 0;
		argDict[\silences] = 0;
		
		postfixstring.do({arg char, i;
			var timestretch, transposition, silences;
			if( (char==$*) || (char==$/), {
				timestretch = "";
				block{|break|
					postfixstring[i+1..postfixstring.size-1].do({arg item; 
						if((item.isAlphaNum) || (item == $.), {timestretch = timestretch ++ item }, {break.value});
					});
				};
				timestretch = timestretch.asFloat;
				if( (char==$/), {timestretch = timestretch.reciprocal; });
				argDict[\timestretch] = timestretch.asFloat;
			});
			if( (char==$+) || (char==$-), {
				transposition = "";
				block{|break|
					postfixstring[i+1..postfixstring.size-1].do({arg item; 
						if(item.isAlphaNum, {transposition = transposition ++ item }, {break.value});
					});
				};
				transposition = transposition.asFloat;
				if( (char==$-), {transposition = transposition.neg; });
				argDict[\transposition] = transposition;
			});
			if( char==$!, {
				silences = "";
				block{|break|
					postfixstring[i+1..postfixstring.size-1].do({arg item; 
						if(item.isAlphaNum, {silences = silences ++ item }, {break.value});
					});
				};
				argDict[\silences] = silences.asInteger;
			});
		});
		
		// -- sustain --
		sustainstartloc = postfixstring.find("(");
		sustainendloc = postfixstring.find(")");
		if(sustainstartloc.isNil, {
			sustainstring = "4";
		}, {
			sustainstring = postfixstring[sustainstartloc+1..sustainendloc-1];
		});
		sustainstring.do({arg dur; var durint;
			durint = dur.asString.asInteger;
			if(durint == 0, {durint = 1}); // make sure durint is not 0 - which will crash the language
			sustainarr = sustainarr.add(TempoClock.default.tempo.reciprocal/durint);
		 });
		argDict.add(\sustainarr -> sustainarr);
		
		// -- attack --
		attacksymbols = postfixstring.findAll("^");
		if(attacksymbols.isNil, {
			attackstring = "5";
		}, {
			attackstartloc = attacksymbols[0];
			attackendloc = attacksymbols[1];
			attackstring = postfixstring[attackstartloc+1..attackendloc-1];
		});
		attackstring.do({arg att; 
			attackarr = attackarr.add(att.asString.asInteger/9); // values range from 0 to 1.0
		 });
		argDict.add(\attackarr -> attackarr);
		^argDict;
	}

	// PERCUSSIVE MODE
	parseScoreMode0 {arg string; 
		var agent, score, splitloc, endchar, agentstring, silenceicon, silences, scorestring, timestretch=1, postfixargs;
		var durarr, sustainarr, spacecount, instrarr, instrstring, quantphase, empty, outbus;
		var attacksymbols, attackstartloc, attackendloc, attackstring, attackarr;
		var startWempty = false;
		var newInstrFlag = false;
		var postfixArgDict;
		
		scorestring = string.reject({arg char; char.ascii == 10 }); // to store in agentDict XXX Testing
		
		string = string.reject({arg char; (char==$-) || (char==$>) || (char.ascii == 10) }); // no need for this here
		splitloc = string.find("|");
		agentstring = string[0..splitloc-1].tr($ , \); // get the name of the agent

		agent = agentstring[0..agentstring.size-1];
		score = string[splitloc+1..string.size-1];
		endchar = score.find("|"); // the index (int) of the end op in the string

		// -- parse the postfix args (after the score)
		postfixargs = score[endchar+1..score.size-1].tr($ , \); // allowing for spaces
		postfixArgDict = this.parsePostfixArgs(postfixargs);
		sustainarr = postfixArgDict.sustainarr;
		attackarr = postfixArgDict.attackarr;
		timestretch = postfixArgDict.timestretch;
		silences = postfixArgDict.silences;

		score = score[0..endchar-1]; // get rid of the function marker
		agent = (docnum.asString++agent).asSymbol;
		
		// -- create a new agent if needed 
		if(agentDict[agent].isNil, {
			agentDict[agent] = [(), ().add(\amp->0.5), nil];
		}, {
			if(agentDict[agent][1].scorestring.contains("{"), { newInstrFlag = true }); // trick to free if the agent was { instr (Pmono is always on)
		}); // 1st = effectRegistryDict, 2nd = scoreInfoDict, 3rd = placeholder for a routine
				
		// ------------- the instrument -------------------
		instrstring = score.tr($ , \);
		instrarr = [];
		instrstring.collect({arg instr, i; instrarr = instrarr.add(instrDict[instr.asSymbol]) });

		// -------------    the score   -------------------
		quantphase=0;	
		spacecount = 0; 
		durarr = [];
		score.do({arg char, i;
			if((char==$ ) && (i==0), { startWempty = true; });
			if(char==$ , {
				spacecount = spacecount+1;
			}, {
				if(i != 0, { // not adding the the first instr
					durarr = durarr.add(spacecount+1); // adding to dur array + include the instr char
					spacecount = 0; 
				}); 
			});
			if(i==(score.size-1), { // fixing at the end of the loop
				durarr = durarr.add(spacecount+1); 
				if(startWempty, {
					quantphase = (durarr[0]-1)/4;
					empty = durarr.removeAt(0)-1; // minus the added instr (since we're only interested in spaces)
					durarr[durarr.size-1] = durarr.last+empty; // adding the last space(s) to the first space(s)
				});
			});
		});

		durarr[durarr.size-1] = durarr.last+silences; // adding silences to the end of the score
		durarr = durarr/4;
		durarr = durarr*timestretch; // duration is stretched by timestretch var
		
		agentDict[agent][1].mode = 0;
		agentDict[agent][1].quantphase = quantphase;
		agentDict[agent][1].durarr = durarr;
		agentDict[agent][1].instrarr = instrarr;
		agentDict[agent][1].sustainarr = sustainarr;
		agentDict[agent][1].attackarr = attackarr;
		agentDict[agent][1].score = score;
		agentDict[agent][1].scorestring = scorestring.asCompileString;
		agentDict[agent][1].instrument = "rhythmtrack";
		agentDict[agent][1].playstate = true;

		{doc.stringColor_(oncolor, doc.selectionStart, doc.selectionSize)}.defer(0.1);  // if code is green (sleeping)
		"------    ixi lang: Created Percussive Agent : ".post; agent.postln; agentDict[agent].postln;
		this.playScoreMode0(agent, durarr, instrarr, sustainarr, attackarr, quantphase, newInstrFlag); 
	}	
	
	// MELODIC MODE
	parseScoreMode1 {arg string;
		var agent, score, scorestartloc, splitloc, endchar, agentstring, instrument, instrstring, timestretch=1, transposition=0;
		var prestring, silenceicon, silences, postfixargs, newInstrFlag = false;
		var durarr, sustainarr, spacecount, notearr, notestring, quantphase, empty, outbus;
		var sustainstartloc, sustainendloc, sustainstring;
		var attacksymbols, attackstartloc, attackendloc, attackstring, attackarr;
		var startWempty = false;
		var channelicon, midichannel;
		var postfixArgDict;
		
		channelicon = 0;

		scorestartloc = string.find("[");
		prestring = string[0..scorestartloc-1].tr($ , \); // get rid of spaces until score
		splitloc = prestring.find("->");
		if(splitloc.isNil, { // no assignment operator
			agent = prestring[0..prestring.size]; // get the name of the agent
			instrument = agent;
		}, {
			agent = prestring[0..splitloc-1]; // get the name of the agent
			instrument = prestring[splitloc+2..prestring.size];
		});
		agent = (docnum.asString++agent).asSymbol;

		score = string[scorestartloc+1..string.size-1];
		endchar = score.find("]"); // the index (int) of the end op in the string
		
		// -- parse the postfix args (after the score)
		postfixargs = score[endchar+1..score.size-1].tr($ , \); // allowing for spaces
		postfixArgDict = this.parsePostfixArgs(postfixargs);
		sustainarr = postfixArgDict.sustainarr;
		attackarr = postfixArgDict.attackarr;
		timestretch = postfixArgDict.timestretch;
		silences = postfixArgDict.silences;
		transposition = postfixArgDict.transposition;
		
		channelicon = score.find("c");
		midichannel = if(channelicon.isNil.not, { score[channelicon+1..channelicon+3].asInteger - 1 }, { 0 });
		
		score = score[0..endchar-1]; // get rid of the function marker
		
		if(agentDict[agent].isNil, {
			agentDict[agent] = [(), ().add(\amp->0.5), nil];
		}, {
			if(agentDict[agent][1].scorestring.contains("{"), {"FREE !!!".postln; newInstrFlag = true }); // trick to free if the agent was { instr (Pmono is always on)
		}); // 1st = effectRegistryDict, 2nd = scoreInfoDict, 3rd = placeholder for a routine

		// ------------- the notes -------------------
		notestring = score.tr($ , \);
		notearr = [];
		notestring.collect({arg note, i;
			var scalenote, thisnote, chord;
			thisnote = note.asString.asInteger; // if thisnote is 0 then it's a chord (since "a".asInteger becomes 0)
			if(thisnote == 0, { // if it is a CHORD
				chordDict.postln;
				chord = chordDict[note.asSymbol];
				if(chord.isNil, { // if using a chord (item in dict) that does not exist (say x) - to prevent error posting
					notearr = notearr.add('\s');
				}, {	
					notearr = notearr.add(chord);
				});
			}, { // it's not a chord but a NOTE
				scalenote = scale[thisnote-1];  // start with 1 as fundamental
				if(scalenote.isNil, {scalenote = scale[(thisnote%scale.size)]+12}); // wrap the scale but add octave
				notearr = notearr.add(scalenote);
			});
		});
		// adding 59 to start with C (and user inputs are 1 as C, 3 as E, 5 as G, etc.)
		notearr = notearr + tonic + transposition; // if added after the score array
		// -------------    the score   -------------------
		quantphase=0;	
		spacecount = 0; 
		durarr = [];
		score.do({arg char, i;
			if((char==$ ) && (i==0), { startWempty = true; });
			if(char==$ , {
				spacecount = spacecount+1;
			}, {
				if(i != 0, { // not adding the the first instr
					durarr = durarr.add(spacecount+1); // adding to dur array + include the instr char
					spacecount = 0; 
				}); 
			});
			if(i==(score.size-1), { // fixing at the end of the loop
				durarr = durarr.add(spacecount+1); 
				if(startWempty, {
					quantphase = (durarr[0]-1)/4;
					empty = durarr.removeAt(0)-1; // minus the added instr (since we're only interested in spaces)
					durarr[durarr.size-1] = durarr.last+empty; // adding the last space(s) to the first space(s)
				});
			});
		});
		durarr[durarr.size-1] = durarr.last+silences; // adding silences to the end of the score
		durarr = durarr/4; 
		durarr = durarr*timestretch; // duration is stretched by timestretch var

		agentDict[agent][1].mode = 1;
		agentDict[agent][1].quantphase = quantphase;
		agentDict[agent][1].durarr = durarr;
		agentDict[agent][1].notearr = notearr;
		agentDict[agent][1].sustainarr = sustainarr;
		agentDict[agent][1].attackarr = attackarr;
		agentDict[agent][1].score = score;
		agentDict[agent][1].scorestring = string.asCompileString;
		agentDict[agent][1].instrument = instrument;
		agentDict[agent][1].midichannel = midichannel;
		agentDict[agent][1].playstate = true;

		{doc.stringColor_(oncolor, doc.selectionStart, doc.selectionSize)}.defer(0.1);  // if code is green (sleeping)
		"------    ixi lang: Created Melodic Agent : ".post; agent.postln; agentDict[agent].postln;
		this.playScoreMode1(agent, notearr, durarr, sustainarr, attackarr, instrument, quantphase, newInstrFlag, midichannel); 
	}	

	// CONCRETE MODE
	parseScoreMode2 {arg string;
		var agent, score, scorestartloc, splitloc, endchar, agentstring, instrument, instrstring, timestretch=1;
		var prestring, silenceicon, silences, postfixargs, newInstrFlag;
		var durarr, spacecount, amparr, ampstring, quantphase, empty, outbus;
		var startWempty = false;
		var postfixArgDict;

		scorestartloc = string.find("{");
		prestring = string[0..scorestartloc-1].tr($ , \); // get rid of spaces until score
		splitloc = prestring.find("->");
		if(splitloc.isNil, { // no assignment operator
			agent = prestring[0..prestring.size]; // get the name of the agent
			instrument = agent;
		}, {
			agent = prestring[0..splitloc-1]; // get the name of the agent
			instrument = prestring[splitloc+2..prestring.size];
		});
		agent = (docnum.asString++agent).asSymbol;
		score = string[scorestartloc+1..string.size-1];
		endchar = score.find("}"); // the index (int) of the end op in the string
		
		// -- parse the postfix args (after the score)
		postfixargs = score[endchar+1..score.size-1].tr($ , \); // allowing for spaces
		postfixArgDict = this.parsePostfixArgs(postfixargs);
		timestretch = postfixArgDict.timestretch;
		silences = postfixArgDict.silences;
		
		score = score[0..endchar-1]; // get rid of the function marker

		// due to Pmono not being able to load a new instr, I check if it there is a new one
		if(agentDict[agent].isNil, { 
			agentDict[agent] = [(), ().add(\amp->0.5), nil];
		}, {			
			if(agentDict[agent][1].instrument != instrument, { 
				"NEW INSTR".postln; 
				newInstrFlag = true;
			}, {
				"SAME INSTR".postln; 
			 	newInstrFlag = false; 
			});	
		});  // 1st = effectRegistryDict, 2nd = scoreInfoDict, 3rd = placeholder for a routine
		
		// ------------- the envelope amps -------------------
		ampstring = score.tr($ , \);
		amparr = [];
		ampstring.do({arg amp; amparr = amparr.add(amp.asString.asInteger/10) });
		// -------------    the score   -------------------
		quantphase=0;	
		spacecount = 0; 
		durarr = [];
		score.do({arg char, i;
			if((char==$ ) && (i==0), { startWempty = true; });
			if(char==$ , {
				spacecount = spacecount+1;
			}, {
				if(i != 0, { // not adding the the first instr
					durarr = durarr.add(spacecount+1); // adding to dur array + include the instr char
					spacecount = 0; 
				}); 
			});
			if(i==(score.size-1), { // fixing at the end of the loop
				durarr = durarr.add(spacecount+1); 
				if(startWempty, {
					quantphase = (durarr[0]-1)/4;
					empty = durarr.removeAt(0)-1; // minus the added instr (since we're only interested in spaces)
					durarr[durarr.size-1] = durarr.last+empty; // adding the last space(s) to the first space(s)
				});
			});
		});
		durarr[durarr.size-1] = durarr.last+silences; // adding silences to the end of the score
		durarr = durarr/4;
		durarr = durarr*timestretch; // duration is stretched by timestretch var

		agentDict[agent][1].mode = 2;
		agentDict[agent][1].quantphase = quantphase;
		agentDict[agent][1].durarr = durarr;
		agentDict[agent][1].amparr = amparr;
		agentDict[agent][1].score = score;		
		agentDict[agent][1].scorestring = string.asCompileString;
		agentDict[agent][1].instrument = instrument;
		agentDict[agent][1].playstate = true; // EXPERIMENTAL: to be used in snapshots

		{doc.stringColor_(oncolor, doc.selectionStart, doc.selectionSize)}.defer(0.1); // if code is green (sleeping)
		"------    ixi lang: Created Concrete Agent : ".post; agent.postln; agentDict[agent].postln;
		this.playScoreMode2(agent, amparr, durarr, instrument, quantphase, newInstrFlag); 
	}	
	
	parseChord {arg string;
		var chordstartloc, splitloc, chordstring, chord, chordname;
		string = string[0..string.size-1].tr($ , \); // get rid of spaces until score
		chordstartloc = string.find("(");
		splitloc = string.find("->");
		chordname = string[0..splitloc-1]; // get the name of the agent
		if(chordname.interpret.isInteger, { // PERCUSSIVE MODE ------------ NOT WORKING - Pseq(\instrument does not expand)
			chordstring = string[splitloc+3..string.size-3];
			chord = [];
			chordstring.do({arg instr; chord = chord.add(instrDict[instr.asSymbol]) });
			chordDict[chordname.asSymbol] = chord; 
			chordDict.postln;
		}, { // MELODIC MODE
			chordstring = string[splitloc+3..string.size-3];
			chord = [];
			chordstring.do({arg note; chord = chord.add(scale[note.asString.asInteger-1]) });
			chordDict[chordname.asSymbol] = chord; 
			chordDict.postln;
		});
	}	

	playScoreMode0 {arg agent, durarr, instrarr, sustainarr, attackarr, quantphase, newInstrFlag;
		// ------------ play function --------------
		if(proxyspace[agent].isNeutral, { // check if the object exists alreay
			Pdef(agent, Pbind(
						\instrument, Pseq(instrarr, inf), 
						\dur, Pseq(durarr, inf),
						\amp, Pseq(attackarr*agentDict[agent][1].amp, inf),
						\sustain, Pseq(sustainarr, inf)
			));
			proxyspace[agent].quant = [durarr.sum, quantphase, 0, 1];
			proxyspace[agent] = Pdef(agent);
			proxyspace[agent].play;
		},{
			if(newInstrFlag, { // only if instrument was {, where Pmono bufferplayer synthdef needs to be shut down
				proxyspace[agent].free; // needed in order to swap instrument in Pmono
				Pdef(agent, Pbind(
							\instrument, Pseq(instrarr, inf), 
							\dur, Pseq(durarr, inf),
							\amp, Pseq(attackarr*agentDict[agent][1].amp, inf),
							\sustain, Pseq(sustainarr, inf)
				)).quant = [durarr.sum, quantphase, 0, 1];
				{ proxyspace[agent].play }.defer(0.5); // defer needed as the free above and play immediately doesn't work
			}, {	// default behavior
				Pdef(agent, Pbind(
							\instrument, Pseq(instrarr, inf), 
							\dur, Pseq(durarr, inf),
							\amp, Pseq(attackarr*agentDict[agent][1].amp, inf),
							\sustain, Pseq(sustainarr, inf)
				)).quant = [durarr.sum, quantphase, 0, 1];
				proxyspace[agent].play; // xxx new
			});
		});
	}
	
	playScoreMode1 {arg agent, notearr, durarr, sustainarr, attackarr, instrument, quantphase, newInstrFlag, midichannel=0;
		if(instrument.asString=="midi", { eventtype = \midi }, { eventtype = \note });
		
		// ------------ play function --------------
		if(proxyspace[agent].isNeutral, { // check if the object exists alreay
			Pdef(agent, Pbind(
						\instrument, instrument, // XXX instrument
						\type, eventtype,
						\midiout, midiclient,
						\chan, midichannel,
						\midinote, Pseq(notearr, inf), 
						\dur, Pseq(durarr, inf),
						\sustain, Pseq(sustainarr, inf),
						\amp, Pseq(attackarr*agentDict[agent][1].amp, inf)
			));
			[\quant, durarr.sum, \quantphase, quantphase].postln;
			proxyspace[agent].quant = [durarr.sum, quantphase, 0, 1];
			proxyspace[agent] = Pdef(agent);
			proxyspace[agent].play;
		},{
			if(newInstrFlag, { // only if instrument was {, where Pmono bufferplayer synthdef needs to be shut down
				proxyspace[agent].free; // needed in order to swap instrument in Pmono
				Pdef(agent, Pbind(
						\instrument, instrument,
						\type, eventtype,
						\midiout, midiclient,
						\chan, midichannel,
						\midinote, Pseq(notearr, inf), 
						\dur, Pseq(durarr, inf),
						\sustain, Pseq(sustainarr, inf),
						\amp, Pseq(attackarr*agentDict[agent][1].amp, inf)
				)).quant = [durarr.sum, quantphase, 0, 1];
				{ proxyspace[agent].play }.defer(0.5); // defer needed as the free above and play immediately doesn't work
			}, { // default behavior
				Pdef(agent, Pbind(
						\instrument, instrument,
						\type, eventtype,
						\midiout, midiclient,
						\chan, midichannel,
						\midinote, Pseq(notearr, inf), 
						\dur, Pseq(durarr, inf),
						\sustain, Pseq(sustainarr, inf),
						\amp, Pseq(attackarr*agentDict[agent][1].amp, inf)
				)).quant = [durarr.sum, quantphase, 0, 1];
				proxyspace[agent].play; // xxx new
			});
		});
	}

	playScoreMode2 {arg agent, amparr, durarr, instrument, quantphase, newInstrFlag;
		// ------------ play function --------------
		if(proxyspace[agent].isNeutral, { // check if the object exists alreay
			Pdefn((agent++"durarray").asSymbol, Pseq(durarr, inf));
			Pdefn((agent++"amparray").asSymbol, Pseq(amparr, inf));
			Pdef(agent, Pmono(instrument, // XXX instrument
						\dur, Pdefn((agent++"durarray").asSymbol),
						\noteamp, Pdefn((agent++"amparray").asSymbol)
			));
			proxyspace[agent].quant = [durarr.sum, quantphase, 0, 1];
			proxyspace[agent] = Pdef(agent);
			proxyspace[agent].play;
		},{
			Pdefn((agent++"durarray").asSymbol, Pseq(durarr, inf)).quant = [durarr.sum, quantphase, 0, 1];
			Pdefn((agent++"amparray").asSymbol, Pseq(amparr, inf)).quant = [durarr.sum, quantphase, 0, 1];
			if(newInstrFlag, {
				proxyspace[agent].free; // needed in order to swap instrument in Pmono
				Pdef(agent, Pmono(instrument, // XXX instrument
							\dur, Pdefn((agent++"durarray").asSymbol),
							\noteamp, Pdefn((agent++"amparray").asSymbol)
				));
				{ proxyspace[agent].play }.defer(0.5); // defer needed as the free above and play immediately doesn't work
			});
		});
		Pdef(agent).set(\amp, agentDict[agent][1].amp); // proxyspace quirk: amp set from outside
	}

	initEffect {arg string;
		var splitloc, agentstring, endchar, agent, effect, effectFoundFlag;
		effectFoundFlag = false;
		string = string.tr($ , \); // get rid of spaces
		string = string.reject({ |c| c.ascii == 10 }); // get rid of char return
		splitloc = string.findAll(">>");		
		agent = string[0..splitloc[0]-1];
		agent = (docnum.asString++agent).asSymbol;
		string = string++$ ;
		endchar = string.find(" ");
		splitloc = splitloc.add(endchar);
		if(groups.at(agent).isNil.not, { // the "agent" is a group
			effect = string[splitloc[0]..splitloc.last];
			groups.at(agent).do({arg agentx, i;
				this.initEffect(agentx++effect); // recursive calling of this method
			});
		}, { // it is a real agent, not a group, then we ADD THE EFFECT
			(splitloc.size-1).do({arg i;
				effect = string[splitloc[i]+2..splitloc[i+1]-1];
				if(agentDict[agent][0][effect.asSymbol].isNil, { // experimental - only add if there is no effect
					agentDict[agent][0][effect.asSymbol] = agentDict[agent][0].size+1;// add 1 (the source is 1)
					if(effectDict[effect.asSymbol].isNil.not, {
						effectFoundFlag = true;
						proxyspace[agent][agentDict[agent][0].size] = \filter -> effectDict[effect.asSymbol];
					});
				});
			});
			" --->    ixi lang : THIS AGENT/GROUP NOW HAS THE FOLLOWING EFFECTS : ".post; agentDict[agent][0].postln;
		});
	}
	
	removeEffect {arg string;
		var splitloc, agentstring, endchar, agent, effect, effectFoundFlag;
		effectFoundFlag = false;
		string = string.tr($ , \); // get rid of spaces
		string = string.reject({ |c| c.ascii == 10 });
 		splitloc = string.findAll("<<");
		agent = string[0..splitloc[0]-1];
		agent = (docnum.asString++agent).asSymbol;
		string = string++$ ;
		endchar = string.find(" ");
		splitloc = splitloc.add(endchar);
		if(groups.at(agent).isNil.not, { // the "agent" is a group
			effect = string[splitloc[0]..splitloc.last];
			groups.at(agent).do({arg agentx, i;
				this.removeEffect(agentx++effect); // recursive calling of same method
			});
		}, { // it is a real agent, not a group
			if(splitloc[0]==(endchar-2), { // remove all effects (if only << is passed)
				" --->    ixi lang : REMOVE ALL EFFECTS".postln;
				10.do({arg i; proxyspace[agent][i+1] =  nil }); // remove all effects (10 max) (+1 as 0 is Pdef)
				agentDict[agent][0].clear;
			}, { // only remove the effects listed (such as agent<<reverb<<tremolo)
				(splitloc.size-1).do({arg i;
					effect = string[splitloc[i]+2..splitloc[i+1]-1];
					if(agentDict[agent][0][effect.asSymbol].isNil.not, { // if the effect exists
						proxyspace[agent][ (agentDict[agent][0][effect.asSymbol]).clip(1,10)] =  nil;
						agentDict[agent][0].removeAt(effect.asSymbol);
					});
				});
			});
			" --->    ixi lang : THIS AGENT/GROUP NOW HAS THE FOLLOWING EFFECTS : ".post; agentDict[agent][0].postln;
		});
	}

	increaseAmp {arg string;
		var splitloc, agentstring, endchar, agent, effect, effectFoundFlag, amp;
		effectFoundFlag = false;
		string = string.tr($ , \); // get rid of spaces
		splitloc = string.find("))");
		agentstring = string[0..splitloc-1]; // get the name of the agent
		agent = agentstring[0..agentstring.size-1];
		agent = (docnum.asString++agent).asSymbol;
		if(groups.at(agent).isNil.not, { // the "agent" is a group
			groups.at(agent).do({arg agentx, i;
				this.increaseAmp(agentx++"))"); // recursive calling of this same method
			});
		}, {				
			amp = agentDict[agent][1].amp;
			amp = (amp + 0.05).clip(0, 2);
			" --->    ixi lang : AMP : ".post; amp.postln;
			agentDict[agent][1].amp = amp;
			switch(agentDict[agent][1].mode)
				{0} { this.playScoreMode0(agent, agentDict[agent][1].durarr, agentDict[agent][1].instrarr, agentDict[agent][1].sustainarr, 
						agentDict[agent][1].attackarr, agentDict[agent][1].quantphase, false); }
				{1} { 
					this.playScoreMode1(agent, agentDict[agent][1].notearr, agentDict[agent][1].durarr, agentDict[agent][1].sustainarr, 
						agentDict[agent][1].attackarr, agentDict[agent][1].instrument, agentDict[agent][1].quantphase, false, 
						agentDict[agent][1].midichannel); }
				{2} { Pdef(agent).set(\amp, amp) };
		});
	}
	
	decreaseAmp {arg string;
		var splitloc, agentstring, endchar, agent, effect, effectFoundFlag, amp;
		effectFoundFlag = false;
		string = string.tr($ , \); // get rid of spaces
		splitloc = string.find("((");
		agentstring = string[0..splitloc-1]; // get the name of the agent
		agent = agentstring[0..agentstring.size-1];
		agent = (docnum.asString++agent).asSymbol;
		if(groups.at(agent).isNil.not, { // the "agent" is a group
			groups.at(agent).do({arg agentx, i;
				this.decreaseAmp(agentx++"(("); // recursive calling of this same method
			});
		}, {
			amp = agentDict[agent][1].amp;
			amp = (amp - 0.05).clip(0, 2);
			" --->    ixi lang : Setting AMP to : ".post; amp.postln;
			agentDict[agent][1].amp = amp;
			switch(agentDict[agent][1].mode)
				{0} { this.playScoreMode0(agent, agentDict[agent][1].durarr, agentDict[agent][1].instrarr, agentDict[agent][1].sustainarr, agentDict[agent][1].attackarr, agentDict[agent][1].quantphase, false); }
				{1} { this.playScoreMode1(agent, agentDict[agent][1].notearr, agentDict[agent][1].durarr, agentDict[agent][1].sustainarr, agentDict[agent][1].attackarr, agentDict[agent][1].instrument, agentDict[agent][1].quantphase, false, agentDict[agent][1].midichannel); }
				{2} { Pdef(agent).set(\amp, amp) };
		});
	}
	
	findStringStartEnd {arg doc, pureagentname;
		var allreturns, stringstart, stringend;
		allreturns = doc.string.findAll("\n");
		
		block{ | break |
			doc.string.findAll(pureagentname).do({arg loc, i;
				stringend = allreturns[allreturns.indexOfGreaterThan(loc)];
				if(doc.string[(loc+pureagentname.size)..stringend].contains("->"), {
					stringstart = loc; //doc.string.find(pureagentname);
					if(pureagentname == doc.string[stringstart..doc.string.findAll("->")[doc.string.findAll("->").indexOfGreaterThan(stringstart+1)]-1].tr($ , \), {
						break.value; //  exact match found and we break loop, leaving stringstart and stringend with correct values
					});
				});
			});
		};
		^[stringstart, stringend];
	}

	swapString {arg doc, pureagentname, newscore, modearray;
		var allreturns, stringstart, stringend, cursorPos, modstring, thisline;
		var scorestringsuffix, transpsuffix, scoremode, scorerange, score, agent;
		#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);
		thisline = doc.string[stringstart..stringend];
		agent = (docnum.asString++pureagentname).asSymbol;

		if(thisline.find("|").isNil.not, {
			scorerange = thisline.findAll("|");
			transpsuffix = thisline[scorerange[1]+1..thisline.size-1]; // if transposition is added
			scoremode = 0;
		});
		if(thisline.find("[").isNil.not, {
			scorerange = [];
			scorerange = scorerange.add(thisline.find("["));
			scorerange = scorerange.add(thisline.find("]"));
			transpsuffix = thisline[scorerange[1]+1..thisline.size-1]; // if transposition is added
			scoremode = 1;
		});
		if(thisline.find("{").isNil.not, {
			scorerange = [];
			scorerange = scorerange.add(thisline.find("{"));
			scorerange = scorerange.add(thisline.find("}"));
			scoremode = 2;
		});
		
		if(modearray[scoremode], { // if this scoremode supports the operation (no need to yoyo a melodic score for example)
			
		score = thisline[scorerange[0]+1..scorerange[1]-1];
		scorestringsuffix = switch(scoremode) {0}{"|"++transpsuffix}{1}{"]"++transpsuffix}{2}{"}"};

		// -------- put it back in place ----------
		modstring = thisline[0..scorerange[0]]++newscore++scorestringsuffix;
		modstring = modstring.reject({ |c| c.ascii == 10 }); // get rid of \n
		{
			{
				cursorPos = doc.selectionStart; // get cursor pos
				#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);
				doc.stringColor_(processcolor, stringstart, stringend-stringstart);
				doc.selectRange(cursorPos); // set cursor pos again
			}.defer;
			0.3.wait;
			{
				cursorPos = doc.selectionStart; // get cursor pos
				#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);
				doc.string_( modstring, stringstart, stringend-stringstart);
				doc.selectRange(cursorPos); // set cursor pos again
			}.defer;
			0.3.wait;
			{
				cursorPos = doc.selectionStart; // get cursor pos
				#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);
				doc.stringColor_(oncolor, stringstart, stringend-stringstart);
				doc.selectRange(cursorPos); // set cursor pos again
				switch(scoremode)
					{0} { if(modearray[0], {
								if(agentDict[agent][1].playstate, {this.parseScoreMode0(modstring)})
							}, {
								"ixi lang : Action not applicable in this mode".postln; 
							}) 
						}
					{1} { if(modearray[1], {
								if(agentDict[agent][1].playstate, {this.parseScoreMode1(modstring)})
							}, {
								"ixi lang : Action not applicable in this mode".postln; 
							}) 
						}
					{2} { if(modearray[2], {
								if(agentDict[agent][1].playstate, {this.parseScoreMode2(modstring)})
							}, {
								"ixi lang : Action not applicable in this mode".postln; 
							}) 
						}
			}.defer;

		}.fork(TempoClock.new);
		
		},{
			"ixi lang : Action not applicable in this mode".postln;		});

	}

	parseMethod {arg string;
		var splitloc, methodstring, spaces, agent, method, pureagentname;
		var thisline, modstring, stringstart, allreturns, stringend, scorerange, score, scoremode, scorestringsuffix;
		var argument, transpsuffix, cursorPos;
		splitloc = string.find(" ");
		methodstring = string[0..splitloc-1].tr($ , \); // get the name of the agent
		method = methodstring[0..methodstring.size-1];
		string = string.reject({ |c| c.ascii == 10 }); // get rid of \n
		string = string++" "; // add a space to the end
		spaces = string.findAll(" ");
		agent = string[splitloc+1..spaces[1]-1];
		pureagentname = agent; // the name of the agent is different in code (0john) and in doc (john) (for multidoc support)
		agent = (docnum.asString++agent).asSymbol;
		if( spaces.size > 1, { argument = string[spaces[1]..spaces[spaces.size-1]] }); // is there an argument?

		// HERE CHECK IF IT'S A GROUP THEN PERFORM A DO LOOP (for each member of the group)
		if(groups.at(agent).isNil.not, { // the "agent" is a group
			groups.at(agent).do({arg agentx, i;
				// "parsing this: ".post; (method+agentx+argument).postln;
				this.parseMethod(method+agentx+argument); // recursive calling of same method
			});
		}, { // it is a real agent, not a group
			
			// -------- find the line in the document -----------
			#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);

			thisline = doc.string[stringstart..stringend];
			// -------- detect which mode it is ---------------
			if(thisline.find("|").isNil.not, {
				scorerange = thisline.findAll("|");
				transpsuffix = thisline[scorerange[1]+1..thisline.size-1]; // if transposition is added
				scoremode = 0;
			});
			if(thisline.find("[").isNil.not, {
				scorerange = [];
				scorerange = scorerange.add(thisline.find("["));
				scorerange = scorerange.add(thisline.find("]"));
				transpsuffix = thisline[scorerange[1]+1..thisline.size-1]; // if transposition is added
				scoremode = 1;
			});
			if(thisline.find("{").isNil.not, {
				scorerange = [];
				scorerange = scorerange.add(thisline.find("{"));
				scorerange = scorerange.add(thisline.find("}"));
				scoremode = 2;
			});
			score = thisline[scorerange[0]+1..scorerange[1]-1];
			scorestringsuffix = switch(scoremode) {0}{"|"++transpsuffix}{1}{"]"++transpsuffix}{2}{"}"};

			// -------------   Perform methods - the ixi lang verbs
			switch(method) 
				{"doze"} {// pause stream
					if(agentDict[agent][1].playstate == true, {
						agentDict[agent][1].playstate = false;
						proxyspace[agent].objects[0].array[0].mute;
				 		doc.stringColor_(offcolor, stringstart, stringend-stringstart);
					})
				}
				{"perk"} { // restart stream
					if(agentDict[agent][1].playstate == false, {
						agentDict[agent][1].playstate = true;
						proxyspace[agent].objects[0].array[0].unmute;
				 		doc.stringColor_(oncolor, stringstart, stringend-stringstart);
					});
				}
				{"nap"} { // pause for either n secs or n secs:number of times
					var napdur, separator, times, on;
					on = true;
					separator = argument.find(":");
					if(separator.isNil.not, { // it contains a on/off order
						napdur = argument[0..separator-1].asInteger;
						// round to even, so it doesn't leave the stream off
						times = argument[separator+1..argument.size-1].asInteger.round(2);
					 	{
					 		(times*2).do({ // times two as the interface is that it should nap twice 
					 			if(on, {
					 				proxyspace[agent].objects[0].array[0].mute;
					 				agentDict[agent][1].playstate = false;
							 		{doc.stringColor_(offcolor, stringstart, stringend-stringstart)}.defer;
					 				on = false;
					 			}, {
					 				proxyspace[agent].objects[0].array[0].unmute;
									agentDict[agent][1].playstate = true;
							 		{doc.stringColor_(oncolor, stringstart, stringend-stringstart)}.defer;
					 				on = true;
					 			});
						 		napdur.wait;
				 			});
					 	}.fork(TempoClock.new);
					}, { // it is just a nap for n seconds and then reawake
					 	{
							argument = argument.asInteger;
				 			proxyspace[agent].objects[0].array[0].mute;
							{doc.stringColor_(offcolor, stringstart, stringend-stringstart)}.defer;
					 		argument.wait;
				 			proxyspace[agent].objects[0].array[0].unmute;
							{doc.stringColor_(oncolor, stringstart, stringend-stringstart)}.defer;
					 	}.fork(TempoClock.new);
				 	});
				}
				{"shake"} { 
					"shaking".postln;
					// -------- perform the method -----------
					score = score.scramble;
					// -------- put it back in place ----------
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{"swap"} {
					var instruments;
					// -------- perform the method -----------
					instruments = score.reject({arg c; c==$ }).scramble;
					score = score.collect({arg char; if(char!=$ , {instruments.pop}, {" "}) });
					
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{">shift"} {
					argument = argument.asInteger;
					// -------- perform the method -----------
					score = score.rotate(if((argument==0) || (argument=="") || (argument==" "), {1}, {argument}));
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{"<shift"} {
					argument = argument.asInteger;	
					// -------- perform the method -----------
					score = score.rotate(if((argument==0) || (argument=="") || (argument==" "), {-1}, {argument.neg}));
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{"invert"} { 
					var temparray;
					// -------- perform the method -----------
					temparray = [];
					score.do({arg char; temparray = temparray.add( if(char==$ , {nil}, {char.asString.asInteger}) ) });
					temparray = temparray.collect({arg val; if(val.isNil.not, { abs(val-8) }) });
					score = "";
					temparray.do({arg item; score = score++if(item==nil, {" "}, {item.asString}) });
					this.swapString(doc, pureagentname, score, [false, true, true]);
				}
				{"expand"} { // has to behave differently as it adds characters
					var tempstring;
					argument = argument.asInteger;
					// -------- perform the method -----------
					if(argument.isNil || (argument==" "), {1}, {argument});
					tempstring = "";
					score.do({arg char; 
						tempstring = tempstring++char;
						if(char!=$ , {
							argument.do({ tempstring = tempstring++" " });
						});
					});
					score = tempstring;
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{"revert"} { // reverse
					// -------- perform the method -----------
					score = score.reverse;
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{"up"} { // all instruments uppercase
					// -------- perform the method -----------
					score = score.toUpper;
					this.swapString(doc, pureagentname, score, [true, false, false]);
				}
				{"down"} { // all instruments lowercase
					// -------- perform the method -----------
					score = score.toLower;								this.swapString(doc, pureagentname, score,[true, false, false]);
				}
				{"yoyo"} { // swaps lowercase and uppercase randomly
					// -------- perform the method -----------
					score = score.collect({arg char; 0.5.coin.if({char.toUpper},{char.toLower})});
					this.swapString(doc, pureagentname, score, [true, false, false]);
				}
				{"order"} { // put things in order in time

				}
				{"remind"} {
					this.getMethodsList;		
				};
		});
	}	
	
	getMethodsList {
		var doc;
		doc = Document.new;
		doc.name_("ixi lang lingo");
		doc.promptToSave_(false);
		doc.background_(Color.black);
		doc.stringColor_(Color.green);
		doc.bounds_(Rect(10, 500, 650, 800));
		doc.font_(Font("Monaco",16));
		doc.string_("
	    --    ixi lang lingo	   --
 
 -----------  score modes  -----------
 |		: percussive score
 [		: melodic score
 {		: concrete score (samples)
  
 -----------  operators  -----------
 ->		: score assigned to an agent
 >> 		: set effect
 << 		: delete effect
 ))		: increase amplitude
 ((		: decrease amplitude
 tonic	: set the tonic
 
 ------------  arguments  ---------
 !		: insert silence (!16 is silence for 16 beats)
 +		: transpose in melodic mode 
 -		: transpose in melodic mode 
 *		: expand the score in all modes 
 /		: contract the score in all modes 
 ()		: control note length (1 is whole note, 2 is half, etc.) 
 ^^		: control note accent (1 is quiet, 9 is loud) 

  -----------  methods  -----------
 doze 	: pause agent
 perk 	: resume agent
 nap		: pause agent for n seconds : n times
 shake 	: randomise the score
 swap 	: swap instruments in score
 >shift 	: right shift array n slot(s)
 <shift 	: left shift array n slot(s)
 invert	: invert the melody
 expand	: expand the score with n nr. of silence(s)
 revert	: revert the order
 order	: organise in time 
 up		: to upper case (in rhythm mode)
 down	: to lower case (in rhythm mode)
 yoyo	: switch upper and lower case (in rhythm mode)
 	
 -----------  commands  -----------
 tempo	: set tempo in bpm (accelerando op: tempo:time)
 future	: set events in future (arg sec:times)
 group	: define a group
 sequence	: define a sequence
 scale	: set scale
 tuning 	: set tuning
 grid  	: draw a line every n spaces 
 remind	: get this document
 instr 	: info on available instruments
 tonality	: info on available scales and tunings
 kill	: stop all sounds in window
 snapshot	 -> : store current state as a snapshot (snapshot -> mySnap)
 snapshot	 mySnap : recall the snapshot
 suicide : allow the language to kill itself sometime in the future
 midiclients : post the available midiclients
 midiout : set the port to the midiclient
 save	: save the session
 load	: load the session
 autocode : autocode some lines
  
 -----------  effects  -----------
 reverb
 reverbS
 reverbL
 delay
 distort
 cyberpunk
 bitcrush
 techno
 technosaw
 antique  
 lowpass
 tremolo
 vibrato
	");
	}
	
	getInstrumentsList {
		var doc;
		doc = Document.new;
		doc.name_("ixi lang instruments");
		doc.promptToSave_(false);
		doc.background_(Color.black);
		doc.stringColor_(Color.green);
		doc.bounds_(Rect(10, 500, 500, 800));
		doc.font_(Font("Monaco",16));
		doc.string_("
	    --    ixi lang instruments    --
 
 ------  melodic instruments  ------\n\n"++
ixiInstr.getMelodicInstr // calling an XiiLangInstr instance
++
"\n\n ------  percussive instruments  ------\n\n"
++
ixiInstr.getPercussiveInstr
	);
	}
	
}


XiiLangSingleton {
	classvar <>windowsList;
	
	*new{ |object|
		^super.new.initXiiLangSingleton(object);
	}
	
	initXiiLangSingleton { |object, windowsCount|
		if(windowsCount == 1, {
			windowsList = [object];
		}, {
			windowsList = windowsList.add(object);
		});
		windowsList.postln;
	}
}


+ String { 
	
	// this is from wslib  /Lang/Improvements/extString-collect.sc
	collect { | function | 
		// it changes functionality of some important methods (like .tr) so I have to include it here
		var result = "";
		this.do {|elem, i| result = result ++ function.value(elem, i); }
		^result;
	}
	
	ixilang { | window=nil, newagent=false |
		if(window.isNil, {
			Document.new.front.string_("XiiLang.new()").string.interpret;
			window = 0;
		});
		XiiLangSingleton.windowsList[window].opInterpreter(this);
		if(newagent, {
			{
			XiiLangSingleton.windowsList[window].doc.string = 			XiiLangSingleton.windowsList[window].doc.string ++ "\n" ++ this ++ "\n";
			}.defer(0.3);
		});
	}
}		


