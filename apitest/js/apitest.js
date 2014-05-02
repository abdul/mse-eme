

apitest = function() {

    var video_element = $("#test_video").get()[0];

    var update_api_status = function($item, is_present)  {
      var present = "glyphicon glyphicon-ok green";
      var missing = "glyphicon glyphicon-remove red";
      $item.addClass((is_present) ? present : missing);
    };

  // MediaKeys
  var mediakeys_present = ("MediaKeys" in window);
  var istypesupp_present = mediakeys_present ? ("isTypeSupported" in MediaKeys) : false;
  update_api_status($("#api_mediakeys"), mediakeys_present);
  if (mediakeys_present) {
    update_api_status($("#api_istypesupp"), istypesupp_present);
  }

  var $api_mediakeys_attr = $("#api_mediakeys_attr");
  if ("mediaKeys" in video_element) {
    update_api_status($api_mediakeys_attr, true);
  }
  else if ("mediaKeys" in window) {
    update_api_status($api_mediakeys_attr, true);
    $("#api_mediakeys_attr_comment").text("attribute found in Window, not in HTMLVideoElement");
  }
  else {
    update_api_status($api_mediakeys_attr, false);
  }

  var $api_setmediakeys = $("#api_setmediakeys");
  if ("setMediaKeys" in video_element) {
    update_api_status($api_setmediakeys, true);
  }
  else if ("setMediaKeys" in window) {
    update_api_status($api_setmediakeys, true);
    $("#api_setmediakeys_comment").text("function found in Window, not in HTMLVideoElement");
  }
  else {
    update_api_status($api_setmediakeys, false);
  }

  // Key Systems
  var key_systems = [
    {
      name: "W3C Clear Key",
      keystring: "org.w3.clearkey",
      dom_id: "cdm_clearkey"
    },
    {
      name: "Microsoft PlayReady",
      keystring: "com.microsoft.playready",
      dom_id: "cdm_playready"
    },
    {
      name: "Google Widevine",
      keystring: "com.widevine.alpha",
      dom_id: "cdm_widevine"
    }
  ];

  var supported_system = null;
  for (i = 0; i < key_systems.length; i++) {

    $("#cdm_table").append("<tr><td>" + key_systems[i].name + "</td>" +
                           "<td><pre>" + key_systems[i].keystring + "</pre></td>" +
                           "<td><span id=\"" + key_systems[i].dom_id + "\">" + "</span></td></tr>");

    if (mediakeys_present && istypesupp_present) {
      if (MediaKeys.isTypeSupported(key_systems[i].keystring)) {
        update_api_status($("#" + key_systems[i].dom_id), true);
        supported_system = key_systems[i].keystring;
      }
      else {
        update_api_status($("#" + key_systems[i].dom_id), false);
      }

    }
    else {
      update_api_status($("#" + key_systems[i].dom_id), false);
    }
  }

  // MediaKeys
  if (supported_system != null) {
    try {
      var mk = new MediaKeys(supported_system);
      update_api_status($("#api_mediakeys_construct"), true);
    } catch (ex) {
      if (ex instanceof ReferenceError) {
        update_api_status($("#api_mediakeys_construct"), false);
        $("#api_mediakeys_construct_comment").text("MediaKeys not present");
      }
      else if (ex instanceof DOMException) {
        update_api_status($("#api_mediakeys_construct"), true);
        $("#api_mediakeys_construct_comment").text("Error = " + ex.name + ". Message = " + ex.message);
      }
    }
  }
};

