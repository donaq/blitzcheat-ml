(function(){
	if(window.hasRun) return;
	window.hasRun = true;
	var x=0,y=0, level=0;

	window.addEventListener("message", function(e){
		var m = e.data;
		if(m.bbcheaty==undefined) return;
		y = m.bbcheaty;
		level = m.level;
	});

	var gameFlasher = setInterval(function(){
		var e = document.getElementById("gameFlash");
		if(!e) return;
		var rect = e.getBoundingClientRect(),
			top = rect.top + y; 
		
		chrome.runtime.sendMessage({top:top,left:rect.left,width:rect.width,height:rect.height});
	}, 5000);;

	var framewalker = setInterval(function(){
		var elems = document.getElementsByTagName("IFRAME"),
			len = elems.length,
			totaly = y;
		// this works only if window is maximised
		if(level==0) totaly += window.screenY + window.outerHeight - window.innerHeight
		for(var i=0;i<len;i++){
			var e = elems[i],
				rect = e.getBoundingClientRect();
			e.contentWindow.postMessage({bbcheaty:totaly+rect.top, level:level+1}, "*");
		}
	}, 2000);
})();
