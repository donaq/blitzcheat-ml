function socksender(){
	var sock = new WebSocket("ws://localhost:9999"),
		timer = {timer:null};
	sock.onopen = function(evt){
		if(timer.timer) clearInterval(timer.timer);
		// perpetually send a timestamp
		timer.timer = setInterval(function(){
			var d = new Date();
			sock.send(''+d.getTime());
		},5000);
	};
	sock.onclose = function(evt){
		if(timer.timer) clearInterval(timer)
		timer.timer = null;
	};
}

socksender();
