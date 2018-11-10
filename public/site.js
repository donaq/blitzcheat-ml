$(document).ready(function(){
    var picdat = null, sortedkeys = null, curr = -1, currclicked = [];

    // save currently clicked point to curr image's class
    function saveclick(){
        if(curr==-1 || currclicked.length<1){
            $("#pointinfo").html("no point to save");
            return;
        }

        var pic = picdat.pics[sortedkeys[curr]],
            clsobj = picdat.classes[pic.class];
        if("areas" in clsobj){
            delete clsobj.areas
        }
        clsobj.click = currclicked[0];
        save();
        draw_points_areas();
    }

    // save current area to curr image's class
    function savearea(){
        if(curr==-1 || currclicked.length<2){
            $("#areainfo").html("no area to save");
            return;
        }

        var x = currclicked[0][0], y = currclicked[0][1],
            height = currclicked[1][1]-y, width = currclicked[1][0]-x,
            area = {"x":x, "y": y, "width":width, "height":height},
            aname = $("#areaname").val();

        currclicked = [];
        if(height<0||width<0){
            $("#areainfo").html("invalid area");
            return;
        }
        if(!aname || aname==""){
            $("#areainfo").html("unnamed area");
            return;
        }
    

        var pic = picdat.pics[sortedkeys[curr]],
            clsobj = picdat.classes[pic.class];

        if("click" in clsobj){
            delete clsobj.click;
        }
        if(!("areas" in clsobj)){
            clsobj.areas = {};
        }
        clsobj.areas[aname] = area;
        save();
        draw_points_areas();
    }

    function clear_points_areas(){
        currclicked = [];
        $(".clicked").remove();
        $(".area").remove();
        $("#pointinfo").html("");
        $("#areainfo").html("");
    }

    function draw_points_areas(){
        clear_points_areas();
        var pic = picdat.pics[sortedkeys[curr]],
            pclass = ("class" in pic)?pic.class:-1,
            highlightcolor = pclass==-1?"red":"powderblue";
        if(pclass==-1){
            if($("#classes").val()==-1) return;
            pclass = $("#classes").val()
        }

        var clsobj = picdat.classes[pclass],
            img = document.getElementById(`image${curr}`);
        if("click" in clsobj){
            drawclicked(img, clsobj.click[0], clsobj.click[1], highlightcolor)
        } else if("areas" in clsobj){
            for(var a in clsobj.areas){
                drawarea(img, a, clsobj.areas[a], highlightcolor);
            }
        }
    }

    function clearimg(){
        // clear previous image stuff
        $(".image").remove();
        clear_points_areas();
    }

    // load image. we only work with curr
    function loadimg(i){
        var k = sortedkeys[i],
            pic = picdat.pics[k],
            img = document.createElement("IMG"),
            container = document.getElementById("imgcontainer");

        curr = i;

        clearimg();

        // adding the image
        img.src = `/raw/${k}`;
        img.className = "image";
        img.id = `image${i}`;
        img.addEventListener('load', e => {
            container.appendChild(img);
            $("#imgname").html(`${k}`);

            // add existing image class
            var pclass = ("class" in pic)?pic.class:$("#classes").val();
            $("#classes").val(pclass);
            $("#imageclass").val($("#classes option:selected").text());

            img.addEventListener("click", img_clicked);

            // draw existing class click point or interesting areas
            draw_points_areas();
            $("#workleft").html((sortedkeys.length-curr)+ " of " + sortedkeys.length + " images left");
        });
    }

    function img_clicked(e){
        var img = e.currentTarget, rect = img.getBoundingClientRect(),
            ileft = rect.left, itop = rect.top,
            // coordinates of click with origin at top left of image
            ix = e.clientX-ileft, iy = e.clientY-itop;

        currclicked.push([ix, iy]);

        drawclicked(img, ix, iy, "powderblue");
    }

    // given an image element and x,y coords, draw a square centered at x,y relative to the image top left corner
    function drawclicked(img, ix, iy, highlightcolor){
        var cdiv = document.createElement("DIV"),
            rect = img.getBoundingClientRect(),
            tmpstyle = {"position": "absolute", "left": (ix-5+rect.left+window.scrollX) + "px", "top": (iy-5+rect.top+window.scrollY) + "px",
                "height":"10px", "width":"10px", "backgroundColor":highlightcolor, "zIndex":10000, "opacity":0.5};

        cdiv.className = "clicked";
        apply_style(cdiv, tmpstyle);
        document.body.appendChild(cdiv);
    }

    function drawarea(img, aname, area, highlightcolor){
        var cdiv = document.createElement("DIV"),
            rect = img.getBoundingClientRect(),
            tmpstyle = {"position": "absolute", "left": (area.x+rect.left+window.scrollX) + "px", "top": (area.y+rect.top+window.scrollY) + "px",
                "height":area.height+"px", "width":area.width+"px", "backgroundColor":highlightcolor, "zIndex":10000, "opacity":0.5};
        cdiv.className = "area";
        cdiv.title = aname;
        apply_style(cdiv, tmpstyle);
        document.body.appendChild(cdiv);
    }

    function apply_style(e, s){ for(var k in s) e.style[k] = s[k]; }

    // populate classification selector
    function populate_classes(){
        $(".classopt").remove();
        var classes = picdat.classes.map(e => e.name);
            cls_sel = document.getElementById("classes");

        var o = document.createElement("option");
        o.className = "classopt";
        o.text = "";
        o.value = "-1";
        cls_sel.appendChild(o);

        classes.forEach((c, i) => {
            var o = document.createElement("option");
            o.className = "classopt";
            o.text = c;
            o.value = parseInt(i);
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

    function savepicclass(){
        var cls = $("#imageclass").val();
        // only do something if we are initialised, we have a value to work with, and we have a pic
        if(!picdat || !cls || cls=="" || curr==-1 || !(curr in sortedkeys)) return;

        var k = sortedkeys[curr];
        // get class index
        var classes = picdat.classes.map(e => e.name),
            cindex = classes.indexOf(cls),
            cv;
        if(cindex!=-1){
            cv = cindex;
        } else {
            cv = picdat.classes.length;
            picdat.classes.push({"name":cls});
            populate_classes();
        }
        // set current pic's class
        picdat.pics[k].class = cv;
        draw_points_areas();

        save();
    }

	function modulo(a,b){
		var r = a % b;
		return r<0?b+r:r;
	}

    // load pics
    $.getJSON("/ls", function(dat){
        picdat = dat;
        sortedkeys = Object.keys(picdat.pics).sort();
        if(!("classes" in picdat)) picdat.classes = [];
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

    $("#next").click(function(){
        if(picdat==null) return;
        savepicclass();
        loadimg(modulo(curr+1, sortedkeys.length));
    });

    $("#prev").click(function(){
        if(picdat==null) return;
        savepicclass();
        loadimg(modulo(curr-1, sortedkeys.length));
    });

    $("#classes").change(evt => {
        $("#imageclass").val($("#classes option:selected").text());
    });

    $("#setclass").click(savepicclass);

    $("#addclick").click(saveclick);

    $("#addarea").click(savearea);
});
