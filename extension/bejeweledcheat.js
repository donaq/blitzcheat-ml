(function(){
	if(window.hasRun) return;
	window.hasRun = true;

	var timer = setInterval(function(){
		var e = document.getElementById("gameFlash");
		if(!e) return;
		var rect = e.getBoundingClientRect();
		chrome.runtime.sendMessage({top:rect.top,left:rect.left,width:rect.width,height:rect.height});
	}, 5000);;
})();
