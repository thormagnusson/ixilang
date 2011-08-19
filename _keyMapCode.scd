	// code used to generate the initial keymappings file
	// map the keys to the names of the soundfiles inside your project folder
	// the project folder is the name of your session (so you start XiiLang("projectname")
	
	/*
	INSTRUCTIONS:
	1) Place THIS document together with your sound samples in the project folder
	2) Enter the names of your soundfiles mapped to the keys
	3) Highlight all (Apple + a)
	4) Hit SHIFT + RETURN (this will generate the _keyMappings.ixi file in your project folder)
	*/


		var instrDict, project; // project is the folder name
		
		instrDict = IdentityDictionary.new;
		project = Document.current.dir.split.last;
		
		
		instrDict[\A] = \ambient;	
		instrDict[\a] = \backswing;
				
		instrDict[\B] = \bassDrone;		
		instrDict[\b] = \bassbow;	
		
		instrDict[\C] = \boom; // \clip;	
		instrDict[\c] = \basscresc;	
		
		instrDict[\D] = \stereoBrook;	
		instrDict[\d] = \bassharm;		
		
		instrDict[\E] = \virotic;	
		instrDict[\e] = \bassnote;	
		
		instrDict[\F] = \cois;
		instrDict[\f] = \bassnote2;	

		instrDict[\G] = \grey;	
		instrDict[\g] = \bassnote3;
		
		instrDict[\H] = \bassnote4;	
		instrDict[\h] = \bassnote4;
		
		instrDict[\I] = \boom;	
		instrDict[\i] = \insec;	

		instrDict[\J] = \camina3;
		instrDict[\j] = \camina4;
	
		instrDict[\K] = \convol1;	
		instrDict[\k] = \convol2;	

		instrDict[\L] = \convol3;	
		instrDict[\l] = \convol4;	
		
		instrDict[\M] = \convol5;	
		instrDict[\m] = \elstatic;	

		instrDict[\N] = \fuzz;	
		instrDict[\n] = \grains;		
		
		instrDict[\O] = \grill;
		instrDict[\o] = \impulse1;
		
		instrDict[\P] = \robotnoise; // \pink;	
		instrDict[\p] = \insec2;	
		
		instrDict[\Q] = \insec3;	
		instrDict[\q] = \machine;	

		instrDict[\R] = \magnetclock;
		instrDict[\r] = \robotnoise;
			
		instrDict[\S] = \rooph;	
		instrDict[\s] = \rooph;	
		
		instrDict[\T] = \sparkup;	
		instrDict[\t] = \speed;
		
		instrDict[\U] = \static;
		instrDict[\u] = \statics;	
		
		instrDict[\V] = \sweetmachine;	
		instrDict[\v] = \viromachine;	

		instrDict[\W] = \white;	
		instrDict[\w] = \windup;	
		
		instrDict[\X] = \woodblock;	
		instrDict[\x] = \yoyo;
		
		instrDict[\Y] = \wheel;	
		instrDict[\y] = \viromachine;	
		
		instrDict[\Z] = \viromachine;	
		instrDict[\z] = \yoyo;	
		
		instrDict.writeArchive("sounds/ixilang/"++project++"/_keyMapping.ixi");

