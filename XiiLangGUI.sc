XiiLangGUI  {

	var win, projectname, key;
	var ixilogo, keyboard, keystring, midivalstring;
	var projectview, projectpath, projectsList;
	var filenames, synthdesclib, synthdefnames, project;
	
	*new { arg projectnamearg;
		^super.new.initGUIXii(projectnamearg);
	}

	initGUIXii { arg projectnamearg;
		var mappingwinfunc;
		
		//projectname = "default";
		//projectpath = "sounds/ixilang/default/";
		projectsList = "sounds/ixilang/*".pathMatch.collect({arg n; n.basename});
		projectname = projectnamearg ? projectsList[0];
	//	[\projectname, projectname].postln;
		projectpath = "sounds/ixilang/"++projectname;
		filenames = (projectpath++"/*").pathMatch;
		filenames = filenames.collect({arg file; file.basename});
		filenames = filenames.reject({ |file| file.splitext[1] == "scd" }); // not including the keymapping files
		filenames = filenames.reject({ |file| file.splitext[1] == "ixi" }); // not including the keymapping files
		filenames = filenames.collect({arg file; file.splitext[0]});

		key = "C";

		ixilogo = [ // the ixi logo
			Point(1,7), Point(8, 1), Point(15,1), Point(15,33),Point(24, 23), Point(15,14), 
			Point(15,1), Point(23,1),Point(34,13), Point(45,1), Point(61,1), Point(66,6), 
			Point(66,37), Point(59,43), Point(53,43), Point(53,12), Point(44,22), Point(53,33), 
			Point(53,43), Point(42,43), Point(34,32),Point(24,43), Point(7,43), Point(1,36), Point(1,8)
			];

		win = SCWindow.new("ixi lang launcher/mapper", Rect(200, 510, 510, 300), resizable:false).front;

		SCStaticText(win, Rect(40, 110, 50, 16)).string_("projects :");
		
		projectview = SCListView(win, Rect(40, 135, 100, 100))
			.items_(projectsList)
			.value_(projectsList.indexOfEqual(projectname))
			.action_({arg view; 
				projectname = projectsList[view.value] ;
				projectpath = "sounds/ixilang/*".pathMatch[view.value];
				projectsList = "sounds/ixilang/*".pathMatch.collect({arg n; n.basename});
				filenames = (projectpath++"*").pathMatch;
				filenames = filenames.collect({arg file; file.basename});
		//		[\projectname, projectname, \projectpath, projectpath].postln;
				filenames = filenames.reject({ |file| file.splitext[1] == "scd" }); // not including the keymapping files
				filenames = filenames.reject({ |file| file.splitext[1] == "ixi" }); // not including the keymapping files
				filenames = filenames.collect({arg file; file.splitext[0]});
			});
		
		SCButton(win, Rect(40, 250, 100, 20))
			.states_([["project folder", Color.black, Color.gray]])
			.action_({ ("open "++projectpath).unixCmd})
			.font_(Font("Helvetica", 11));

		keyboard = MIDIKeyboard(win, Rect(190, 135, 280, 60), 3, 48)
			.keyDownAction_({arg note;
				key = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"][note%12];
				"Key (tonic) : ".post; note.postln;
				keyboard.clear;
				keyboard.setColor(note, Color.green.alpha_(0.3));
				keystring.string_(	["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"].wrapAt(note));
				midivalstring.string_(note.asString);
			}).setColor(60, Color.green.alpha_(0.3));
		
		SCStaticText(win, Rect(190, 210, 50, 16)).string_("key:");
		keystring = SCStaticText(win, Rect(220, 210, 50, 16)).string_("C");
		SCStaticText(win, Rect(250, 210, 60, 16)).string_("midivalue:");
		midivalstring = SCStaticText(win, Rect(320, 210, 50, 16)).string_(60);
		
		SCStaticText(win, Rect(190, 10, 350, 120))
			.string_("Put short samples into a folder within the ixilang sounds folder.\nThe name you give that folder becomes the project name. \nYou can map the keys on your keyboard to the sample names.\n\nIf you just want to test, without creating a mapping for a new \nproject, press 'start session'.")
			.font_(Font("Helvetica", 10));

		SCButton(win, Rect(190, 250, 80, 20))
			.states_([["map keys", Color.black, Color.gray]])
			.action_({ mappingwinfunc.value })
			.font_(Font("Helvetica", 11));
		
		SCButton(win, Rect(290, 250, 80, 20))
			.states_([["ixi lang help", Color.black, Color.gray]])
			.action_({ XiiLang.openHelpFile })
			.font_(Font("Helvetica", 11));

		SCButton(win, Rect(390, 250, 80, 20))
			.states_([["start session", Color.black, Color.green.alpha_(0.2)]])
			.action_({ 
				XiiLang.new(projectname, key, true, true);
			})
			.font_(Font("Helvetica", 11));

		win.drawHook = {
			GUI.pen.color = Color.new255(255, 102, 0, 160);
			GUI.pen.width = 3;
			GUI.pen.translate(47,33);
			GUI.pen.scale(1.2,1.2);
			GUI.pen.moveTo(1@7);
			ixilogo.do({arg point;
				GUI.pen.lineTo(point+0.5);
			});
			GUI.pen.stroke;
		};

		mappingwinfunc = {
			var win, column, letters, synthDefDict, dictFound, savebutt;

		
		synthdesclib = SynthDescLib(projectname.asSymbol);
		("sounds/ixilang/"++projectname++"/synthdefs.scd").loadPath;
		SynthDescLib.read;
		synthdefnames = SynthDescLib.getLib(projectname.asSymbol).synthDescs.keys.asArray;
		[\synthdefnames, synthdefnames].postln;
			
			if(Object.readArchive(projectpath++"/_keyMapping.ixi").isNil, {
				synthDefDict = IdentityDictionary.new;
				dictFound = false;
			}, {
				synthDefDict = Object.readArchive(projectpath++"/_keyMapping.ixi");
				dictFound = true;
			});
		
			XiiLangInstr.new(projectname);
		
			column = 0;
			letters = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz";
			win = SCWindow.new("mapping keyboard keys to samples - project: "+projectname.quote, Rect(710,410, 460, 760), resizable:false).front;
		
			win.view.keyDownAction_({arg view, cha, modifiers, unicode, keycode;
				if(letters.includes(cha), {
					{Synth(synthDefDict[cha.asSymbol])}.value;
				});
				letters.do({arg char, i; // thanks julio
					if(char == cha, {
						win.view.children[1+(i*4)].focus(true);
					});
				});
			});

			letters.do({arg char, i; var j, mapname;
				if(dictFound.not, {
					synthDefDict[char.asSymbol] = filenames.wrapAt(i);
				});
				column = (i/26).floor*220;
				j = i % 26;
				SCStaticText(win, Rect(15+column, ((j+1)*26)+1, 200, 15)).background_(Color.grey.alpha_(0.5));
				SCPopUpMenu(win, Rect(15+column, (j+1)*26, 15, 16))
					.items_(filenames)
					.action_({arg view; 
						mapname.string_(filenames[view.value]);
						synthDefDict[char.asSymbol] = filenames[view.value];
						savebutt.value_(0);
					})
					.keyDownAction_({arg view, cha, modifiers, unicode, keycode; 
						switch(keycode)
							{123}{ view.valueAction_(view.value-1); savebutt.value_(0); }
							{124}{ view.valueAction_(view.value+1); savebutt.value_(0); }
							{126}{ view.valueAction_(view.value-1); savebutt.value_(0); }
							{125}{ view.valueAction_(view.value+1); savebutt.value_(0); }
							{51}{ 
								win.view.children.do({arg vw, i;
									if(vw === view, {
										if(i>3, {
											win.view.children[i-4].focus(true);
										})
									})
								})
							}
							{36}{ Synth(synthDefDict[char.asSymbol]) };
					});
				SCPopUpMenu(win, Rect(34+column, (j+1)*26, 15, 16))
					.items_(synthdefnames)
					.action_({arg view; 
						mapname.string_(synthdefnames[view.value]);
						synthDefDict[char.asSymbol] = synthdefnames[view.value];
						savebutt.value_(0);
					})
					.keyDownAction_({arg view, cha, modifiers, unicode, keycode; 
						switch(keycode)
							{123}{ view.valueAction_(view.value-1); savebutt.value_(0); }
							{124}{ view.valueAction_(view.value+1); savebutt.value_(0); }
							{126}{ view.valueAction_(view.value-1); savebutt.value_(0); }
							{125}{ view.valueAction_(view.value+1); savebutt.value_(0); }
							{51}{ 
								win.view.children.do({arg vw, i;
									if(vw === view, {
										if(i>3, {
											win.view.children[i-5].focus(true);
										})
									})
								})
							}
							{36}{ Synth(synthDefDict[char.asSymbol]) };
					});
				SCStaticText(win, Rect(58+column, (j+1)*26, 70, 16))
					.string_(char++" ->")
					.font_(Font("Monaco", 11));
				mapname = SCStaticText(win, Rect(96+column, (j+1)*26, 140, 16))
							.string_(
								if(dictFound, {
									synthDefDict[char.asSymbol];
								}, {
									filenames.wrapAt(i);
								})
							)
							.font_(Font("Monaco", 11));
			});
			
			SCStaticText(win, Rect(15, 710, 200, 40))
				.background_(Color.grey.alpha_(0.5))
				.string_(" Use arrow keys, TAB, Delete and \n ENTER to navigate sample library")
				.font_(Font("Monaco", 9));
		
			savebutt = SCButton(win, Rect(235, 710, 200, 20))
				.states_([["save mapping file", Color.black, Color.green.alpha_(0.2)], ["saved", Color.black, Color.clear]])
				.action_({ 
					synthDefDict.writeArchive(projectpath++"/_keyMapping.ixi");
					" ---> ixi lang NOTE: You need to restart your session for the new mapping to take function".postln;
				})
				.font_(Font("Helvetica", 11));
															win.view.children[1].focus(true);

		}

	}

}

