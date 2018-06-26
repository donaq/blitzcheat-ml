var sock = null;

function createsock(){
	sock = new WebSocket("ws://localhost:9999");
	sock.onclose = function(evt){
		createsock();
	};
	sock.onerror = function(evt){
		console.log(evt);
		sock.close();
		createsock();
	};
}

createsock();

chrome.runtime.onMessage.addListener(function(msg, cb){
	//TODO: check if window and tab is active. if not, return
	// check if socket is open
	if(!sock || sock.readyState!=WebSocket.OPEN) return;

	sock.send(JSON.stringify(msg));
	console.log("sent " + msg);
});
