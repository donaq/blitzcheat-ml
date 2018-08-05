$(document).ready(function(){
    var picdat = null, sortedkeys = null, curr = -1;

    // load image. we only work with curr
    function loadimg(i){
        var k = sortedkeys[i],
            pic = picdat.pics[k],
            img = document.createElement("IMG"),
            container = document.getElementById("imgcontainer");

        curr = i;

        // adding the image
        $(".image").empty();
        img.src = `/raw/${k}`;
        img.className = "image";
        img.id = `image${i}`;
        container.appendChild(img);
        $("#imgname").html(`${k}`);

    }

    // populate classification selector
    function populate_classes(){
        $(".classopt").empty();
        var classes = Object.keys(picdat.classes).sort(),
            cls_sel = document.getElementById("classes");

        classes.forEach(c => {
            var o = document.createElement("option");
            o.className = "classopt";
            o.text = c;
            o.value = c;
            cls_sel.appendChild(o);
        });
    }

    function save(){
        $.ajax({
            url:"/annotate",
            type:"POST",
            data: JSON.stringify(picdat),
            contentType: 'application/json; charset=utf-8',
            success: function(){ console.log("success!"); }
        });
    }

    // load pics
    $.getJSON("/ls", function(dat){
        picdat = dat;
        sortedkeys = Object.keys(picdat.pics).sort();
        if(!("classes" in picdat)) picdat.classes = {};
        populate_classes();
    });

    // go to first pic that's not annotated
    $("#first").click(function(){
        if(picdat==null) return;
        var pics = picdat.pics;
        for(var i=0;i<sortedkeys.length;i++){
            var k = sortedkeys[i];
            if(!$.isEmptyObject(pics[k])) continue;
            loadimg(i);
            return;
        }
        loadimg(0);
    });

    $("#classes").change(evt => {
        $("#imageclass").val($("#classes").val());
    });

    $("#setclass").click(function(){
        var cls = $("#imageclass").val();
        // only do something if we are initialised, we have a value to work with, and we have a pic
        if(!picdat || !cls || cls=="" || curr==-1 || !(curr in sortedkeys)) return;

        var k = sortedkeys[curr];
        // set current pic's class
        picdat.pics[k].class = cls;

        // add this class if it doesn't exist
        if(!(cls in picdat.classes)){
            picdat.classes[cls] = {};
            populate_classes();
        }

        save();
    });
});