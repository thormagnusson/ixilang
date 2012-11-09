XiiLangGUI  {

	var win, projectname, key;
	var ixilogo, keyboard, keystring, midivalstring;
	var projectview, projectpath, projectsList;
	var filenames, synthdesclib, synthdefnames, project;
	var recorder, thisversion;
	var numChan;

	*new { arg projectnamearg, numChannels=2;
		^super.new.initGUIXii(projectnamearg, numChannels);
	}

	initGUIXii { arg projectnamearg, numChannels;
		var mappingwinfunc;
		thisversion = 3;
		this.addixiMenu;
		
		projectsList = "ixilang/*".pathMatch.collect({arg n; n.basename});
		projectname = projectnamearg ? projectsList[0];
		projectpath = "ixilang/"++projectname;
		filenames = (projectpath++"/samples/*").pathMatch;
		filenames = filenames.collect({arg file; file.basename});
		filenames = filenames.reject({ |file| file.splitext[1] == "scd" }); // not including the keymapping files
		filenames = filenames.reject({ |file| file.splitext[1] == "ixi" }); // not including the keymapping files
		filenames = filenames.collect({arg file; file.splitext[0]});
		numChan = numChannels;

		key = "C";

		ixilogo = [ // the ixi logo
			Point(1,7), Point(8, 1), Point(15,1), Point(15,33),Point(24, 23), Point(15,14), 
			Point(15,1), Point(23,1),Point(34,13), Point(45,1), Point(61,1), Point(66,6), 
			Point(66,37), Point(59,43), Point(53,43), Point(53,12), Point(44,22), Point(53,33), 
			Point(53,43), Point(42,43), Point(34,32),Point(24,43), Point(7,43), Point(1,36), Point(1,8)
			];

		win = Window.new("ixi lang launcher/mapper", Rect(200, 510, 510, 300), resizable:false).front;

		StaticText(win, Rect(40, 110, 50, 16)).string_("projects :");
		
		projectview = ListView(win, Rect(40, 135, 120, 100))
			.items_(projectsList)
			.value_(projectsList.indexOfEqual(projectname))
			.action_({arg view; 
				projectname = projectsList[view.value] ;
				projectpath = "ixilang/*".pathMatch[view.value];
				projectsList = "ixilang/*".pathMatch.collect({arg n; n.basename});
				filenames = (projectpath++"/samples/*").pathMatch;
				filenames = filenames.collect({arg file; file.basename});
				filenames = filenames.reject({ |file| file.splitext[1] == "scd" }); // not including the keymapping files
				filenames = filenames.reject({ |file| file.splitext[1] == "ixi" }); // not including the keymapping files
				filenames = filenames.collect({arg file; file.splitext[0]});
			});
		
		Button(win, Rect(40, 250, 80, 20))
			.states_([["project folder", Color.black, Color.gray]])
			.action_({ [\projectpath, projectpath].postln; ("open "++projectpath).unixCmd})
			.font_(Font("Helvetica", 11));

		Button(win, Rect(124, 250, 36, 20))
			.states_([["rec", Color.black, Color.red.alpha_(0.2)], ["stop", Color.black, Color.red.alpha_(0.4)]])
			.action_({arg butt;
				if(butt.value == 1, {
					recorder = XiiLangRecord(Server.default, 0, numChan, 'aiff', 'int16');
					[\path, projectpath++"/recordings/"++projectname++"_"++Date.getDate.stamp.asString++".aif"].postln;
					recorder.start(projectpath++"/recordings/"++projectname++"_"++Date.getDate.stamp.asString++".aif");
				}, {
					recorder.stop;
				});
			})
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
		
		StaticText(win, Rect(190, 210, 50, 16)).string_("key:");
		keystring = StaticText(win, Rect(220, 210, 50, 16)).string_("C");
		StaticText(win, Rect(250, 210, 60, 16)).string_("midivalue:");
		midivalstring = StaticText(win, Rect(310, 210, 50, 16)).string_(60);

		StaticText(win, Rect(350, 210, 100, 16)).string_("nr of channels:");
		PopUpMenu(win, Rect(440, 210, 30, 16)).items_({|i| (i+1).asString }!8).value_(1).action_({arg menu; numChan = menu.value+1; });
		
		StaticText(win, Rect(190, 10, 350, 120))
			.string_("-> The folders within the ixilang folder are your projects.\n-> The name of the folder is your project name.\n-> To create your own project, simply copy the 'default' folder \nand rename it.\n\nIf you just want to test, without creating a mapping for a new \nproject, click the 'start' button. Have fun!")
			.font_(Font("Helvetica", 10));

		Button(win, Rect(190, 250, 65, 20))
			.states_([["colors", Color.black, Color.gray]])
			.action_({ [\projectname, projectname].postln; XiiLangColors.new(projectname) })
			.font_(Font("Helvetica", 11));

		Button(win, Rect(262, 250, 65, 20))
			.states_([["map keys", Color.black, Color.gray]])
			.action_({ mappingwinfunc.value })
			.font_(Font("Helvetica", 11));
		
		Button(win, Rect(334, 250, 65, 20))
			.states_([["help", Color.black, Color.gray]])
			.action_({
				// XiiLang.openHelpFile // ixi lang does not have the new helpfile format
				(XiiLang.filenameSymbol.asString.dirname++"/XiiLang.html").openTextFile;
			})
			.font_(Font("Helvetica", 11));

		Button(win, Rect(406, 250, 65, 20))
			.states_([["start", Color.black, Color.green.alpha_(0.2)]])
			.action_({ 
				XiiLang.new(projectname, key, false, true, numChannels:numChan);
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
			var ixiLangInstr;
		
		synthdesclib = SynthDescLib(projectname.asSymbol);
		("ixilang/"++projectname++"/synthdefs.scd").load;
		SynthDescLib.read;
		synthdefnames = SynthDescLib.getLib(projectname.asSymbol).synthDescs.keys.asArray;
			if(Object.readArchive(projectpath++"/keyMapping.ixi").isNil, {
				synthDefDict = IdentityDictionary.new;
				dictFound = false;
			}, {
				synthDefDict = Object.readArchive(projectpath++"/keyMapping.ixi");
				dictFound = true;
			});
		
			ixiLangInstr = XiiLangInstr.new(projectname, true, numChan);
		
			column = 0;
			letters = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz";
			win = Window.new("mapping keyboard keys to samples - project: "+projectname.quote, Rect(710,410, 460, 766), resizable:false).front;
			win.onClose_({
				ixiLangInstr.freeBuffers;			
			});
			win.view.keyDownAction_({arg view, cha, modifiers, unicode, keycode;
				if(letters.includes(cha), {
					{fork{
						var a;
						a = Synth(synthDefDict[cha.asSymbol], [\freq, 60.midicps]);
						0.5.wait;
						a.release;
						}
					}.value;
				});
				letters.do({arg char, i; // thanks julio
					if(char == cha, {
						win.view.children[((1+i)*5)].focus(true);
					});
				});
			});

			StaticText(win, Rect(19, 8, 20, 15)).string_("1");
			StaticText(win, Rect(37, 8, 20, 15)).string_("2");
			StaticText(win, Rect(240, 8, 20, 15)).string_("1");
			StaticText(win, Rect(258, 8, 20, 15)).string_("2");

			letters.do({arg char, i; var j, mapname;
				if(dictFound.not, {
					synthDefDict[char.asSymbol] = filenames.wrapAt(i);
				});
				column = (i/26).floor*220;
				j = i % 26;
				StaticText(win, Rect(15+column, ((j+1)*26)+1, 200, 15)).background_(Color.grey.alpha_(0.5));
				// sample synthdefs
				PopUpMenu(win, Rect(15+column, (j+1)*26, 15, 16))
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
										if(i>4, {
											win.view.children[i-4].focus(true);
										})
									})
								})
							}
							{36}{												{fork{
									var a;
									a = Synth(synthDefDict[char.asSymbol], [\freq, 60.midicps]);
									0.5.wait;
									a.release;
									}
								}.value;
							}
					});
				// synthesis synthdefs
				PopUpMenu(win, Rect(34+column, (j+1)*26, 15, 16))
					.items_(synthdefnames)
					.action_({arg view; 
						if(synthdefnames.size!=0, {
							mapname.string_(synthdefnames[view.value]);
							synthDefDict[char.asSymbol] = synthdefnames[view.value];
							savebutt.value_(0);
						});
					})
					.keyDownAction_({arg view, cha, modifiers, unicode, keycode; 
						if(synthdefnames.size!=0, {
							switch(keycode)
								{123}{ view.valueAction_(view.value-1); savebutt.value_(0); }
								{124}{ view.valueAction_(view.value+1); savebutt.value_(0); }
								{126}{ view.valueAction_(view.value-1); savebutt.value_(0); }
								{125}{ view.valueAction_(view.value+1); savebutt.value_(0); }
								{51}{ 
									win.view.children.do({arg vw, i;
										if(vw === view, {
											if(i>3, {
												win.view.children[i-1].focus(true);
											})
										})
									})
								}
								{36}{ {{var x; x = Synth(synthDefDict[char.asSymbol], [\freq, 60.midicps]); 0.5.wait; x.release; }.fork}.value };
						});
					});
				StaticText(win, Rect(58+column, (j+1)*26, 70, 16))
					.string_(char++" ->")
					.font_(Font("Monaco", 11));
				mapname = StaticText(win, Rect(96+column, (j+1)*26, 140, 16))
							.string_(
								if(dictFound, {
									synthDefDict[char.asSymbol];
								}, {
									filenames.wrapAt(i);
								})
							)
							.font_(Font("Monaco", 11));
			});
			
			StaticText(win, Rect(15, 710, 200, 45))
				.background_(Color.grey.alpha_(0.5))
				.string_(" Use arrow keys, TAB, Delete and \n ENTER to navigate sample library\n 1 = samples, 2 = synths")
				.font_(Font("Monaco", 9));
		
			savebutt = Button(win, Rect(235, 710, 200, 20))
				.states_([["save mapping file", Color.black, Color.green.alpha_(0.2)], ["saved", Color.black, Color.clear]])
				.action_({ 
					synthDefDict.writeArchive(projectpath++"/keyMapping.ixi");
					" ---> ixi lang NOTE: You need to restart your session for the new mapping to take function (Press the green 'Start session' button".postln;
				})
				.font_(Font("Helvetica", 11));
															win.view.children[1].focus(true);

		}

	}
	
	addixiMenu {
		
		var a, createMenu = true;
		
		//CocoaMenuItem.clearCustomItems;
		CocoaMenuItem.topLevelItems.do({arg item; if(item.name == "ixi", { createMenu = false })});
		
		if(createMenu, {
			a = SCMenuGroup(nil, "ixi", 10);
			
			SCMenuItem(a, "Feedback")
				.action_({
					"open http://www.ixi-audio.net/ixilang/ixilang_feedback.html".unixCmd;
				});
			SCMenuSeparator(a, 1); // add a separator
	
			SCMenuItem(a, "Survey")
				.action_({
			"open http://www.ixi-audio.net/ixilang/survey".unixCmd;
				});
			SCMenuSeparator(a, 3); // add a separator
				
			SCMenuItem(a, "Check for Update")
				.action_({
					var latestversion, pipe;
					// check if user is online: (will return a 4 digit number if not)
					a = "curl http://www.ixi-audio.net/ixilang/version.txt".systemCmd;
					// then get the version number (from a textfile with only one number in it)
					if(a==0, {
						pipe = Pipe.new("curl http://www.ixi-audio.net/ixilang/version.txt", "r");
						latestversion = pipe.getLine; 
						pipe.close;
						[\latestversion, latestversion, \thisversion, thisversion].postln;
						if(latestversion.asFloat > thisversion, {
							XiiAlert.new("New version (version "++latestversion++") is available on the ixi website");
							{"open http://www.ixi-audio.net".unixCmd}.defer(2.5); // allow for time to read
						}, {
							XiiAlert.new("You have got the latest version of ixi lang");
						});
					});
				});
		});
	}


}

