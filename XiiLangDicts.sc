XiiLangDicts {
	
	*new { arg language;
		^super.new.initXiiLangDict(language);
	}
		
	*getDict {arg language;
		var langs = ( 
		
			\icelandic : 	( 'flokkur': "group", 'blunda': "nap", 'hrista': "shake", 'skipta': "swap", 'upp': "up", 'nidur': "down"),
			\danish : 	( 'gruppe': "group", 'blunda': "nap", 'hrista': "shake", 'skipta': "swap", 'upp': "up", 'nidur': "down"),
			\spanish : 	( 'grupo': "group", 'secuencia': "sequence", 'futuro': "future", 'foto': "snapshot", 'tempo': "tempo", 'escala': "scale",
							'empujarescala': "scalepush", 'afinaci—n': "tuning", 'empujarafinaci—n': "scalepush", 'recordar': "remind",
							'tonality': "tonalidad", 'instr': "instr", 'notaclave': "tonic", 'red': "grid", 'kill': "matar", 'dormir': "doze",
							'espabilar': "perk", 'nap': "siesta", 'agitar': "shake", 'intercambiar': "swap", '>desplazar': ">shift", 
							'<desplazar': "<shift", 'invertir': "invert", 'expandir': "expand", 'revertir': "revert", 'arriba': "up",
							'abajo': "down", 'yoyo': "yoyo", 'ordenar': "order", 'suicidar': "suicide", 'dicc': "dict", 'clientsmidi': "midiclients",
							'salamidi': "midiout")
		
		
		);
		
		^langs[language.asSymbol];
		
	}
	
	*getList {arg language;
		var langs = ( 
		
			\icelandic : ["flokkur", "sequence", "future", "snapshot", "->", "))", "((", "|", "[", "{", "(", ".", ">>", "<<", "tempo", 
						"scale", "scalepush", "tuning", "tuningpush", "remind", "tonality", "instr", "tonic", "grid", "kill",  
						"blunda", "perk", "nap", "hrista", "skipta", ">shift", "<shift", "invert", "expand", "reverse", 
						"upp", "nidur", "yoyo", "order", "suicide", "dict", "midiclients", "midiout"],

			\danish : ["gruppe", "sekvens", "fremtid", "snapshot", "->", "))", "((", "|", "[", "{", "(", ".", ">>", "<<", "tempo", 
						"skale", "skaletryk", "stilling", "stillingtryk", "minne", "tonalitet", "instr", "tonic", "grid", "draebe",  
						"sove", "vaagne", "nap", "riste", "bytte", ">shift", "<shift", "invert", "expandere", "reverse", 
						"op", "ned", "yoyo", "ordne", "selvmord", "dict", "midiklients", "midiud"],
						
			\spanish : ["grupo", "secuencia", "futuro", "foto", "->", "))", "((", "|", "[", "{", "(", ".", ">>", "<<", "tempo", 
						"escala", "empujarescala", "afinaci—n", "empujarafinaci—n", "recordar", "tonalidad", "instr", "notaclave", 
						"red", "matar", "dormir", "espabilar", "siesta", "agitar", "intercambiar", ">desplazar", "<desplazar", 
						"invertir", "expandir", "revertir", "arriba", "abajo", "yoyo", "ordenar", "suicidar", "dicc", 
						"clientesmidi", "salidamidi"]

				
				);

		^langs[language.asSymbol];
		
	}
	
	post {
		"posting".postln;	
	}
	
}

/*
["group", "sequence", "future", "snapshot", "->", "))", "((", "|", "[", "{", "(", ".", ">>", "<<", "tempo", 
"scale", "scalepush", "tuning", "tuningpush", "remind", "tonality", "instr", "tonic", "grid", "kill",  
"doze", "perk", "nap", "shake", "swap", ">shift", "<shift", "invert", "expand", "reverse", 
"up", "down", "yoyo", "order", "suicide", "dict", "midiclients", "midiout"];
*/