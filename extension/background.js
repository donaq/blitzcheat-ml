var sock = null;

function createsock(){
	sock = new WebSocket("ws://localhost:9999");
	sock.onclose = function(evt){
		sock = null;
	};
	sock.onerror = function(evt){
		console.log(evt);
		sock.close();
		sock = null;
	};
}

createsock();

chrome.runtime.onMessage.addListener(function(msg, sender, cb){
	//TODO: check if window and tab is active. if not, return
	var tab = sender.tab,
		winid = tab.windowId,
		active = tab.active;

	if(!active) return;

	chrome.windows.get(winid, function(w){
		// coordinates are only accurate if window is maximised
		if(!w.focused || w.state!="maximized") return;
		// check if socket is open
		if(!sock || sock.readyState!=WebSocket.OPEN) return;

		sock.send(JSON.stringify(msg));
	});
});
