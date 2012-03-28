
XiiLangColors {
	
		
	*new { arg projectnamearg;
		^super.new.initXiiLangColors(projectnamearg);
	}

	initXiiLangColors { arg projectname;
	
		var doc, win;
		var doccolor, oncolor, activecolor, offcolor, deadcolor;
		[\projectnameCOL, projectname].postln; 
		try{ // check if the color file exists
			#doccolor, oncolor, activecolor, offcolor, deadcolor = Object.readArchive("ixilang/"++projectname++"/colors.ixi")
		};
		
		if(doccolor.isNil, { 
			doccolor = Color.black;
			oncolor = Color.white;
			activecolor = Color.yellow;
			offcolor = Color.green;
			deadcolor = Color.red;
		});
		
		doc = Document.new;
		doc.background_(doccolor);
		doc.stringColor_(oncolor);
		doc.font_(Font("Monaco",20));
		doc.promptToSave_(false);		
		doc.bounds_(Rect(100, 500, 700, 600));
		doc.string_("




// string color
jimi -> string[1   3   2   3   ] +12 <123488>


// active color (when agent is changing)
jimi -> string[1   3   2   3   ] +12 <123488>


// dozing color (when agent is dozing)
jimi -> string[1   3   2   3   ] +12 <123488>


// dead color (when agent is dead)
jimi -> string[1   3   2   3   ] +12 <123488>


		");
		doc.stringColor_(oncolor, 0, 68);
		doc.stringColor_(activecolor, 68, 88);
		doc.stringColor_(offcolor, 156, 86);
		doc.stringColor_(deadcolor, 244, 86);
		
		doc.editable_(false);
		
		
		win = Window.new("ixi lang Doc Color Scheme", Rect(810, 500, 300, 580), resizable:false).front; 
		
		// document
		StaticText(win, Rect(100, 20, 100, 16)).string_("document color");
		EZSlider(win, Rect(0, 40, 280, 15), "red     ", [0, 255].asSpec).action_({arg sl;
			doccolor = Color.new255(sl.value, doccolor.green*255, doccolor.blue*255);
			doc.background_(doccolor);
		}).value_(doccolor.red*255);
		EZSlider(win, Rect(0, 60, 280, 15), "green     ", [0, 255].asSpec).action_({arg sl;
			doccolor = Color.new255(doccolor.red*255, sl.value, doccolor.blue*255);
			doc.background_(doccolor);
		}).value_(doccolor.green*255);
		EZSlider(win, Rect(0, 80, 280, 15), "blue     ", [0, 255].asSpec).action_({arg sl;
			doccolor = Color.new255(doccolor.red*255, doccolor.green*255, sl.value);
			doc.background_(doccolor);
		}).value_(doccolor.blue*255);
	
		// string
		StaticText(win, Rect(100, 120, 100, 16)).string_("text color");
		EZSlider(win, Rect(0, 140, 280, 15), "red     ", [0, 255].asSpec).action_({arg sl;
			oncolor = Color.new255(sl.value, oncolor.green*255, oncolor.blue*255);
			doc.stringColor_(oncolor, 0, 68);
		}).value_(oncolor.red*255);
		EZSlider(win, Rect(0, 160, 280, 15), "green     ", [0, 255].asSpec).action_({arg sl;
			oncolor = Color.new255(oncolor.red*255, sl.value, oncolor.blue*255);
			doc.stringColor_(oncolor, 0, 68);
		}).value_(oncolor.green*255);
		EZSlider(win, Rect(0, 180, 280, 15), "blue     ", [0, 255].asSpec).action_({arg sl;
			oncolor = Color.new255(oncolor.red*255, oncolor.green*255, sl.value);
			doc.stringColor_(oncolor, 0, 68);
		}).value_(oncolor.blue*255);
	
		// active
		StaticText(win, Rect(100, 220, 100, 16)).string_("active string color");
		EZSlider(win, Rect(0, 240, 280, 15), "red     ", [0, 255].asSpec).action_({arg sl;
			activecolor = Color.new255(sl.value, activecolor.green*255, activecolor.blue*255);
			doc.stringColor_(activecolor, 68, 88);
		}).value_(activecolor.red*255);
		EZSlider(win, Rect(0, 260, 280, 15), "green     ", [0, 255].asSpec).action_({arg sl;
			activecolor = Color.new255(activecolor.red*255, sl.value, activecolor.blue*255);
			doc.stringColor_(activecolor, 68, 88);
		}).value_(activecolor.green*255);
		EZSlider(win, Rect(0, 280, 280, 15), "blue     ", [0, 255].asSpec).action_({arg sl;
			activecolor = Color.new255(activecolor.red*255, activecolor.green*255, sl.value);
			doc.stringColor_(activecolor, 68, 88);
		}).value_(activecolor.blue*255);
	
		// dozing
		StaticText(win, Rect(100, 320, 100, 16)).string_("dozing string color");
		EZSlider(win, Rect(0, 340, 280, 15), "red     ", [0, 255].asSpec).action_({arg sl;
			offcolor = Color.new255(sl.value, offcolor.green*255, offcolor.blue*255);
			doc.stringColor_(offcolor, 156, 86);
		}).value_(offcolor.red*255);
		EZSlider(win, Rect(0, 360, 280, 15), "green     ", [0, 255].asSpec).action_({arg sl;
			offcolor = Color.new255(offcolor.red*255, sl.value, offcolor.blue*255);
			doc.stringColor_(offcolor, 156, 86);
		}).value_(offcolor.green*255);
		EZSlider(win, Rect(0, 380, 280, 15), "blue     ", [0, 255].asSpec).action_({arg sl;
			offcolor = Color.new255(offcolor.red*255, offcolor.green*255, sl.value);
			doc.stringColor_(offcolor, 156, 86);
		}).value_(offcolor.blue*255);
	
		// dead
		StaticText(win, Rect(100, 420, 100, 16)).string_("dead string color");
		EZSlider(win, Rect(0, 440, 280, 15), "red     ", [0, 255].asSpec).action_({arg sl;
			deadcolor = Color.new255(sl.value, deadcolor.green*255, deadcolor.blue*255);
			doc.stringColor_(deadcolor, 244, 86);
		}).value_(deadcolor.red*255);
		EZSlider(win, Rect(0, 460, 280, 15), "green     ", [0, 255].asSpec).action_({arg sl;
			deadcolor = Color.new255(deadcolor.red*255, sl.value, deadcolor.blue*255);
			doc.stringColor_(deadcolor, 244, 86);
		}).value_(deadcolor.green*255);
		EZSlider(win, Rect(0, 480, 280, 15), "blue     ", [0, 255].asSpec).action_({arg sl;
			deadcolor = Color.new255(deadcolor.red*255, deadcolor.green*255, sl.value);
			doc.stringColor_(deadcolor, 244, 86);
		}).value_(deadcolor.blue*255);
	
		Button(win, Rect(20, 530, 260, 30))
			.states_([["save color file", Color.black, Color.green.alpha_(0.2)]])
			.action_({ 
				
			[doccolor, oncolor, activecolor, offcolor, deadcolor].writeArchive("ixilang/"++projectname++"/colors.ixi");
				" ---> ixi lang NOTE: You need to restart your session for the new mapping to take function (Press the green 'Start session' button".postln;
			})
			.font_(Font("Helvetica", 11));
		
		doc.onClose_({win.close});
		win.onClose_({doc.close});

		
	}
}
