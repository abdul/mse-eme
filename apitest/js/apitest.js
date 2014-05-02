

apitest = function() {

  var video_element = $("#test_video").get()[0];

  var update_api_status = function($item, is_present)  {
    var present = "glyphicon glyphicon-ok green";
    var missing = "glyphicon glyphicon-remove red";
    $item.addClass((is_present) ? present : missing);
  };

  // MediaKeys (check vendor-specific prefixes too)
  var stdmediakeys_present = ("MediaKeys" in window);
  var msmediakeys_present = ("MSMediaKeys" in window);
  var webkitmediakeys_present = ("WebKitMediaKeys" in window);
  var mediakeys_present = (stdmediakeys_present || msmediakeys_present || webkitmediakeys_present);
  var mediakeys;

  if (stdmediakeys_present) {
    update_api_status($("#api_mediakeys"), true);
    mediakeys = "MediaKeys";
  } else if (msmediakeys_present) {
    update_api_status($("#api_mediakeys"), true);
    $("#api_mediakeys_comment").text("Prefixed MSMediaKeys only")
    mediakeys = "MSMediaKeys";
  } else if (webkitmediakeys_present) {
    update_api_status($("#api_mediakeys"), true);
    $("#api_mediakeys_comment").text("Prefixed WebKitMediaKeys only")
    mediakeys = "WebKitMediaKeys";
  } else {
    update_api_status($("#api_mediakeys"), false);
  }

  var istypesupp_present = false;
  if (mediakeys_present) {
    istypesupp_present =  ("isTypeSupported" in window[mediakeys]);
    update_api_status($("#api_istypesupp"), istypesupp_present);
  }

  var $api_mediakeys_attr = $("#api_mediakeys_attr");
  if ("mediaKeys" in video_element) {
    update_api_status($api_mediakeys_attr, true);
  } else if ("mediaKeys" in window) {
    update_api_status($api_mediakeys_attr, true);
    $("#api_mediakeys_attr_comment").text("Attribute found in Window, not in HTMLVideoElement");
  } else {
    update_api_status($api_mediakeys_attr, false);
  }

  var $api_setmediakeys = $("#api_setmediakeys");
  if ("setMediaKeys" in video_element) {
    update_api_status($api_setmediakeys, true);
  } else if ("msSetMediaKeys" in video_element) {
    update_api_status($api_setmediakeys, true);
    $("#api_setmediakeys_comment").text("Prefixed msSetMediaKeys only");
  } else if ("WebKitSetMediaKeys" in video_element) {
    update_api_status($api_setmediakeys, true);
    $("#api_setmediakeys_comment").text("Prefixed WebKitSetMediaKeys only");
  } else if ("setMediaKeys" in window) {
    update_api_status($api_setmediakeys, true);
    $("#api_setmediakeys_comment").text("Function found in Window, not in HTMLVideoElement");
  } else if ("msSetMediaKeys" in window) {
    update_api_status($api_setmediakeys, true);
    $("#api_setmediakeys_comment").text("Prefixed msSetMediaKeys function found in Window, not in HTMLVideoElement");
  } else if ("WebKitSetMediaKeys" in window) {
    update_api_status($api_setmediakeys, true);
    $("#api_setmediakeys_comment").text("Prefixed WebSetMediaKeys function found in Window, not in HTMLVideoElement");
  } else {
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
      if (stdmediakeys_present) {
        if (MediaKeys.isTypeSupported(key_systems[i].keystring)) {
          update_api_status($("#" + key_systems[i].dom_id), true);
          supported_system = key_systems[i].keystring;
        } else {
          update_api_status($("#" + key_systems[i].dom_id), false);
        }
      } else if (msmediakeys_present) {

      }

    } else {
      update_api_status($("#" + key_systems[i].dom_id), false);
    }
  }

  // MediaKeys
  if (supported_system != null) {
    try {
      var mk = new window[mediakeys](supported_system);
      update_api_status($("#api_mediakeys_construct"), true);
    } catch (ex) {
      if (ex instanceof ReferenceError) {
        update_api_status($("#api_mediakeys_construct"), false);
        $("#api_mediakeys_construct_comment").text("MediaKeys not present");
      } else if (ex instanceof DOMException) {
        update_api_status($("#api_mediakeys_construct"), true);
        $("#api_mediakeys_construct_comment").text("Error = " + ex.name + ". Message = " + ex.message);
      }
    }
  }
};

