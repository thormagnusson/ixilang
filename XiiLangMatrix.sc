
/*
XiiLangMatrix.new(10)
*/

XiiLangMatrix {	

	var doc, string, matrix, codeDoc, start, drawMatrix, cursorPos, goto, row, col;
	var charDict, matrixCopy, clockArray;
	
	*new { arg size=6, direction=\x, instrDict, doccol, strcol;
		^super.new.initXiiLangMatrix( size, direction, instrDict, doccol, strcol );
	}

	initXiiLangMatrix {arg size, direction, instrDict, adoccol, astrcol;
		var doccol, strcol;
		doccol = adoccol ? Color.black;
		strcol = astrcol ? Color.white;
		
		row = 0;
		col = 0;
		
		charDict = ();
		clockArray = [];
		
		matrix = {arg i; {arg j;
			().add(\char -> ".")
			.add(\instr -> "none")
			.add(\note -> 60)
			.add(\amp -> 0.4)
			.add(\wait -> 1)
			.add(\nextX -> if(direction == \y, {j}, {((j+1)%size)}))
			.add(\nextY -> if(direction == \y, {((i+1)%size)}, {i}))
			.add(\sccode -> "{}")
			.add(\code -> "	
			instr  : none
			note   : 60
			amp    : 0.1
			wait   : 1
			nextX  : 1
			nextY  : 1
			sccode : {}
		")
		}!size}!size; // put an empty dict into a 2d array
		
		TempoClock.default.addDependant(this);

		doc = Document.new("matrix")
				.background_(doccol)
				.bounds_(Rect(200, 500, 50*size, 52*size))
				.stringColor_(strcol)
				.font_(Font("Monaco",30))
				.promptToSave_(false)
				.keyDownAction_({arg doc, key, mod, unicode, keycode;
					var loc, loccol, locrow, selsize;
					//[doc, key, mod, unicode, keycode].postln;
					loc = doc.selectedRangeLocation;
					selsize = doc.selectionSize;
					loccol = ((loc%((size*2)+1))/2).floor;
					locrow = (loc/((size*2)+1)).floor;
					if(loccol==size, {loccol = loccol-1});
					
					{doc.title_("matrix"+loccol.asString+locrow.asString)}.defer(0.1);
					
					if(key.isAlpha, { // all chars possible, no arrows and nums
						if((doc.string(loc) == " "), { // no char into a space
							doc.string_(" ", loc, 2);
						}, {
							doc.string_("", loc, 1);	
						});
						if((mod == 8388864) || (mod == 8388608) || (mod == 8519938) || (mod == 10486016), { // holding down fn (and optinally shift) opens a coding window
							if(matrix[locrow][loccol].instr == "none", { // just insert the synthdef from the instrDict
								matrix[locrow][loccol].instr  = instrDict[key.asSymbol];
								// no other info needed.
							});
							codeDoc.value(loccol, locrow, key, true); // imposed key does not exist 
						},{ // default is keys from instrDict (as mapped by instrDict)
							matrix[locrow][loccol].char = key;
							// parsing the code
							if(charDict[key.asSymbol].isNil, {
								matrix[locrow][loccol].instr  = instrDict[key.asSymbol];
							}, {
								matrix[locrow][loccol].instr  = charDict[key.asSymbol].instr;
								matrix[locrow][loccol].sccode  = charDict[key.asSymbol].sccode;
//								[\code, matrix[locrow][loccol].sccode].postln;
							});
							matrixCopy = matrix.copy;
							// store the latest info in the charDict, so it will be set on next key
							charDict[key.asSymbol] = ().add(\instr -> matrix[locrow][loccol].instr)
											.add(\note -> matrix[locrow][loccol].note)
											.add(\amp -> matrix[locrow][loccol].amp)
											.add(\wait -> matrix[locrow][loccol].wait)
											.add(\sccode -> matrix[locrow][loccol].sccode);
						});
					});
					if(key.isDecDigit, { // if number, then it sets the nextX or nextY (depending on direction)
						if(direction == \x, {
							matrix[locrow][loccol].nextX = key.asString.asInteger;
						}, {
							matrix[locrow][loccol].nextY = key.asString.asInteger;
						});
					});
					if(key == $., { // full stop will remove instrument but keep timing and code
						if((doc.string(loc) == " "), { // no char into a space
							doc.string_(" ", loc, 2);
						}, {
							doc.string_("", loc, 1);	
						});
						matrix[locrow][loccol].instr = "none";
						matrix[locrow][loccol].char = ".";
					});
					if(selsize==1, {
						codeDoc.value(loccol, locrow, key, true); // force open the code window
					}); 
					if(keycode == 48, { // use TAB to start a tempoclock
						start.value(loccol, locrow);
					});
					if(keycode == 51, { // use DELETE to stop the last tempoclock
						clockArray.last.stop;
						clockArray.removeAt(clockArray.size-1);
					});
				})
				.onClose_({
					clockArray.do({arg clock; clock.clear });
					TempoClock.default.removeDependant(this);
				});
		
		drawMatrix = {arg row=0, col=0, wait=1, except=false;
			var charholder;
			charholder = if(matrix[row][col].char!=$@, { matrix[row][col].char }, {matrixCopy[row][col].char});
		
			if(except == false, { // when starting, no @ sign
				matrix[row][col].char = $@;
			});
			
			cursorPos = doc.selectionStart; // get cursor pos
			string = " ";
			matrix.do({arg row, j; 
				row.do({arg cell, i;
					if(((i+1)%size)==0, {
						string = string++cell.char; // original
					},{
						string = string++cell.char++" "; // original
					});
				});
				string = string ++ "\n ";
			});
			doc.string_(""); 
			doc.string_(string);
			doc.selectRange(cursorPos); // set cursor pos again
			// the below: in a busy matrix, another clock might have drawn @. Need to 
			{matrix[row][col].char = if(charholder==$@, {matrixCopy[row][col].char}, {charholder})}.defer(wait/4);
		};
		
		drawMatrix.value(except:true);
						
		codeDoc = {arg argcol, argrow, thiskey, forceopen=false;
			// note - this is the only place where matrix numbers are from 1 to n (no zero)
			var col, row;
			col = argcol;
			row = argrow;
		
	//	[\chardict, charDict[thiskey.asSymbol]].postln;
		
			if(charDict[thiskey.asSymbol].isNil || forceopen, { // if new or forceopen - a window and load a dict
			Document.new("matrix code win" + col + row + matrix[row][col].char)
				.background_(doccol)
				.stringColor_(strcol)
				.font_(Font("Monaco",20))
				.promptToSave_(false)
				.keyDownAction_({arg doc, char, mod, unicode, keycode; // test synthdef
					if((mod & 524288 == 524288) && ((keycode==124)||(keycode==123)||(keycode==125)||(keycode==126)), { // alt + arrow keys
						Synth(doc.selectedString.asSymbol);
						//"ixi lang: Testing synthdef : ".post; doc.selectedString.postln;
					});
				})
				.string_("	
	instr  : "++ matrix[row][col].instr ++ "
	note   : "++ matrix[row][col].note ++ "
	amp    : "++ matrix[row][col].amp ++ "
	wait   : "++ matrix[row][col].wait ++ "
	nextX  : "++ (matrix[row][col].nextX) ++ "
	nextY  : "++ (matrix[row][col].nextY) ++ "
	sccode : "++ matrix[row][col].sccode ++"\n"
				)
				.selectRange(55,0) // place the cursor in the middle
				.onClose_({arg doc;
					var str = doc.string;
					var instr;
					matrix[row][col].sccode = str;
					matrix[row][col].char = thiskey;
					// parsing the code
					
					instr  = str[str.find("instr  :")+9..str.findAll("\n")[1]-1];
					instr = instr.collect({arg char; if((char == Char.nl) || (char == Char.space), {""}, {char}) });
					matrix[row][col].instr = instr.asSymbol;
					matrix[row][col].note   = str[str.find("note   :")+9..str.findAll("\n")[2]-1].asFloat;
					matrix[row][col].amp    = str[str.find("amp    :")+9..str.findAll("\n")[3]-1].asFloat;
					matrix[row][col].wait   = str[str.find("wait   :")+9..str.findAll("\n")[4]-1].asFloat;
					matrix[row][col].nextX  = (str[str.find("nextX  :")+9..str.findAll("\n")[5]-1].asInteger);
					matrix[row][col].nextY  = (str[str.find("nextY  :")+9..str.findAll("\n")[6]-1].asInteger);
					matrix[row][col].sccode = str[str.find("sccode :")+9..str.findAll("\n")[str.findAll("\n").size]];
					matrixCopy = matrix.copy;
					charDict[thiskey.asSymbol] = ().add(\instr -> matrix[row][col].instr)
											   .add(\note -> matrix[row][col].note)
											   .add(\amp -> matrix[row][col].amp)
											   .add(\wait -> matrix[row][col].wait)
											   .add(\sccode -> matrix[row][col].sccode);
					// if(matrix[row][col].instr == \none, { matrix[row][col].char = $. });
				});
			},{ // else, the charDict has the instrument and instructions are automatically loaded
					matrix[row][col].instr  = charDict[thiskey.asSymbol].instr;
					matrix[row][col].note   = charDict[thiskey.asSymbol].note;
					matrix[row][col].amp    = charDict[thiskey.asSymbol].amp;
					matrix[row][col].wait   = charDict[thiskey.asSymbol].wait;
					matrix[row][col].sccode = charDict[thiskey.asSymbol].sccode;
			});
		};
		
		start = {arg col=0, row=0;
			clockArray = clockArray.add(
			TempoClock.new(TempoClock.default.tempo, TempoClock.default.beats)
				.schedAbs(TempoClock.nextTimeOnGrid,{
			//	{
				var instr, freq, amp, char, wait;
				var charholder, scCodeFlag;
				var tempcol, temprow; // necessary
				var code;
				
				scCodeFlag = false;
				tempcol = col;
				temprow = row;
				
				~instr = nil;
				~note  = nil;
				~amp   = nil;
				~wait  = nil;
				~nextX = nil;
				~nextY = nil;
				
//				matrix[row][col].sccode.interpret.value; // code interpreted that might overwrite the nils above


				instr = ~instr ? matrix[row][col].instr;
				freq  = ~note  ? matrix[row][col].note;
				freq  = freq.midicps;
				amp   = ~amp   ? matrix[row][col].amp;
				char  = ~char  ? matrix[row][col].char;
				wait  = ~wait  ? matrix[row][col].wait;
				
				{arg row, col, wait;
					{drawMatrix.value(row, col, wait)}.defer; // drawer
				}.value(row, col, wait);
				
				if(char!=".", {
					{	code = matrix[row][col].sccode;
						{code.interpret.value }.defer(0.2);
					}.value; // store this in a wrapper because row and col will be different in 0.1 s time
					if(instr.asString.size != 0, { // prevent server complaining about synthdef not found (empty one)
						Server.default.sendBundle(0.2, ['/s_new', instr, Server.default.nextNodeID, 0, 1, \freq, freq, \amp, amp]);
					});
				});

				col = ~nextX ? matrix[temprow][tempcol].nextX;
				row = ~nextY ? matrix[temprow][tempcol].nextY;

				wait;
				//}.value;
			});
			);
			//clockArray.postln;
		};
	}
	
	update {|theChanger, what, moreArgs|
		if(what == \tempo, {
			this.setTempo_(theChanger.tempo);	
		});
	}

	setTempo_ {arg newtempo;
		clockArray.do({arg clock; clock.tempo = newtempo });
	}
}




