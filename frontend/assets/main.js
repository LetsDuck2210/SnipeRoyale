function refresh() {
    let xhttp = new XMLHttpRequest();
    xhttp.open(
        method="GET",
        url="/change", // new clans
        async=true
    );
    xhttp.send();
    xhttp.onreadystatechange = () => {
        if(xhttp.readyState == 4) {
            if(!isNaN(parseInt(xhttp.responseText))) {
                console.log("Refresh-error.cl: " + xhttp.responseText);
                return;
            }

            document.getElementById('clanlist').innerHTML += xhttp.responseText;
        }
    };
    
    refresh_checked();

	let checked = parseInt(document.getElementById('checked-clans').innerHTML);
	let total = parseInt(document.getElementById('total-clans').innerHTML)
	if(checked < total)
    	setTimeout(refresh, 1000);
}
function refresh_checked() {
    let xhttp = new XMLHttpRequest();
    xhttp.open(
        method="GET",
        url="/changec", // checked clans
        async=true
    );
    xhttp.send();
    xhttp.onreadystatechange = () => {
        if(xhttp.readyState == 4) {
            if(isNaN(parseInt(xhttp.responseText))) {
                console.log("Refresh-error.tc: " + xhttp.responseText);
                return;
            }

            document.getElementById('checked-clans').innerHTML = xhttp.responseText
        }
    };
}

function getCookie(cname) {
  let name = cname + "=";
  let decodedCookie = decodeURIComponent(document.cookie);
  let ca = decodedCookie.split(';');
  for(let i = 0; i <ca.length; i++) {
    let c = ca[i];
    while (c.charAt(0) == ' ') {
      c = c.substring(1);
    }
    if (c.indexOf(name) == 0) {
      return c.substring(name.length, c.length);
    }
  }
  return "";
}