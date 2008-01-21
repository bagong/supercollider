
// see MIDIResponder help for all classes on this page


MIDIResponder {
	var	<>function,<>swallowEvent=false,
		<>matchEvent;		// for matching ports, channels, and parameters
	init { arg install;
		if(this.class.initialized.not,{ this.class.init });
		matchEvent.port = matchEvent.port.asMIDIInPortUID;
		if(install,{this.class.add(this);});
	}
	respond { arg src,chan,num,value;
		if(this.match(src,chan,num,value),{
			this.value(src,chan,num,value)
			^swallowEvent
		});
		^false;
	}
	match { arg src,chan,num,value;
		^matchEvent.match(src,chan,num,value);
	}
	value { arg src,chan,a,b;
		function.value(src, chan, a, b)
	}

	remove {
		this.class.remove(this)
	}
	*removeAll { 
		if(this == MIDIResponder,{
			this.subclasses.do({ |responderClass| responderClass.removeAll })
		},{
			this.init 
		})
	}
}



NoteOnResponder : MIDIResponder {
	classvar <norinit = false,<nonr;

	*new { arg function, src, chan, num, veloc, install=true,swallowEvent=false;
		^super.new.function_(function)
			.matchEvent_(MIDIEvent(nil, src, chan, num, veloc))
			.swallowEvent_(swallowEvent)
			.init(install)
	}
	*initialized { ^norinit }
	*responders { ^nonr }
	*init {
		if(MIDIClient.initialized.not,{ MIDIIn.connect });
		norinit = true;
		nonr = [];
		MIDIIn.noteOn = { arg src, chan, note, veloc;
			nonr.any({ arg r;
				r.respond(src,chan,note,veloc)
			});
		}
	}
	*add { arg resp;
		nonr = nonr.add(resp);
	}
	*remove { arg resp;
		nonr.remove(resp);
	}
}

NoteOffResponder : NoteOnResponder {
	classvar <noffinit = false,<noffr;

	*init {
		if(MIDIClient.initialized.not,{ MIDIIn.connect });
		noffinit = true;
		noffr = [];
		MIDIIn.noteOff = { arg src, chan, note, veloc;
			noffr.any({ arg r;
				r.respond(src,chan,note,veloc)
			});
		}
	}
	*initialized { ^noffinit }
	*responders { ^noffr }

	*add { arg resp;
		noffr = noffr.add(resp);
	}
	*remove { arg resp;
		noffr.remove(resp);
	}
}

CCResponder : MIDIResponder {
	classvar <ccinit = false,<ccr,<ccnumr;

	*new { arg function, src, chan, num, value, install=true,swallowEvent=false;
		^super.new.function_(function).swallowEvent_(swallowEvent)
			.matchEvent_(MIDIEvent(nil, src, chan, num, value))
			.init(install)
	}
	*initialized { ^ccinit }
	*responders { ^ccnumr.select(_.notNil).flat ++ ccr }
	*add { arg resp;
		var temp;
		if(this.initialized.not,{ this.init });
		if((temp = resp.matchEvent.ctlnum).isNumber) {
			ccnumr[temp] = ccnumr[temp].add(resp);
		} {
			ccr = ccr.add(resp);
		};
	}
	*remove { arg resp;
		var temp;
		if((temp = resp.matchEvent.ctlnum).isNumber) {
			ccnumr[temp].remove(resp)
		} {
			ccr.remove(resp);
		};
	}
	*init {
		if(MIDIClient.initialized.not,{ MIDIIn.connect });
		ccinit = true;
		ccr = [];
		ccnumr = Array.newClear(128);
		MIDIIn.control = { arg src,chan,num,val;
			// first try cc num specific
			// then try non-specific (matches any cc )
			[ccnumr[num], ccr].any({ |stack|
				stack.notNil and: {stack.any({ |r| r.respond(src,chan,num,val) })}
			})
		};
	}
}

TouchResponder : MIDIResponder {
	classvar <touchinit = false,<touchr;

	*new { arg function, src, chan, value, install=true,swallowEvent=false;
		^super.new.function_(function).swallowEvent_(swallowEvent)
			.matchEvent_(MIDIEvent(nil, src, chan, nil, value))
			.init(install)
	}
	*init {
		if(MIDIClient.initialized.not,{ MIDIIn.connect });
		touchinit = true;
		touchr = [];
		MIDIIn.touch = { arg src, chan, val;
			touchr.any({ arg r;
				r.respond(src,chan,nil,val)
			})
		}
	}
	value { arg src,chan,num,val;
		// num is irrelevant
		function.value(src,chan,val);
	}
	*initialized { ^touchinit }
	*responders { ^touchr }

	*add { arg resp;
		touchr = touchr.add(resp);
	}
	*remove { arg resp;
		touchr.remove(resp);
	}
}

BendResponder : TouchResponder {
	classvar <bendinit = false,<bendr;

	*init {
		if(MIDIClient.initialized.not,{ MIDIIn.connect });
		bendinit = true;
		bendr = [];
		MIDIIn.bend = { arg src, chan, val;
			bendr.any({ arg r;
				r.respond(src,chan,nil,val)
			});
		}
	}
	*initialized { ^bendinit }
	*responders { ^bendr }

	*add { arg resp;
		bendr = bendr.add(resp);
	}
	*remove { arg resp;
		bendr.remove(resp);
	}
}

/*
NoteOnOffResponder
	the note on function would return an object which is stored.
	when a matching note off event occurs, the object is passed into the note off function
PolyTouchResponder
*/

