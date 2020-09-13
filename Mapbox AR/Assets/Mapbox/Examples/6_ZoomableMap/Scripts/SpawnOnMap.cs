namespace Mapbox.Examples
{
	using UnityEngine;
	using Mapbox.Utils;
	using Mapbox.Unity.Map;
	using Mapbox.Unity.MeshGeneration.Factories;
	using Mapbox.Unity.Utilities;
	using System.Collections.Generic;
    using UnityEngine.UI;

    public class SpawnOnMap : MonoBehaviour
	{
		[SerializeField]
		AbstractMap _map;

		[SerializeField]
		[Geocode]
		string[] _locationStrings;
		Vector2d[] _locations;

		[SerializeField]
		float _spawnScale = 100f;

		[SerializeField]
		GameObject _markerPrefab;

		List<GameObject> _spawnedObjects;
        public Text t;
        string importedData;

        private void OnEnable()
        {
            //importedData = PlayerPrefs.GetString("latLongData");
        }

        void Start()
		{
            //_locationStrings[0] = PlayerPrefs.GetString("latLongData"); ;

            //here we will read from the other application.
            //https://medium.com/@angelhiadefiesta/integrate-a-unity-module-to-a-native-android-app-87644fe899e0
            //AndroidJavaClass UnityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            //AndroidJavaObject currentActivity = UnityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            //AndroidJavaObject intent = currentActivity.Call<AndroidJavaObject>("getIntent");
            //bool hasExtra = intent.Call<bool>("hasExtra", "arguments");

            /*if (hasExtra)
            {
                Debug.Log("has extra");
                AndroidJavaObject extras = intent.Call<AndroidJavaObject>("getExtras");
                string arguments = extras.Call<string>("getString", "arguments");
                _locationStrings[1] = arguments;
                t.text = "Arguments are::"+arguments;
            }
            else
            {
                Debug.Log("no extra");
                t.text = "NO ARGUMENTS FOUND";
            }*/




            //stop here

            //getting data from the main BaatCheat android application
            //https://answers.unity.com/questions/1327186/how-to-get-intent-data-for-unity-to-use.html
            t.text += "here1";
            AndroidJavaClass UnityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            t.text += "here2";
            AndroidJavaObject currentActivity = UnityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            t.text += "here3";
            AndroidJavaObject intent = currentActivity.Call<AndroidJavaObject>("getIntent");
            t.text += "here4";
            t.text+=intent.ToString();/*
            string arguments = intent.Call<string>("getDataString", "latLong");
            t.text = "here5";
            if (intent != null) {
                t.text = "We have got some arguments!"+arguments;
                //t.text = arguments;
                _locationStrings[0] = arguments;
            }             
            else
                t.text = "No Arg found";*/
            /*
            bool hasExtra = intent.Call<bool>("hasExtra", "arguments");

            if (hasExtra)
            {
                Debug.Log("has extra");
                AndroidJavaObject extras = intent.Call<AndroidJavaObject>("getExtras");
                string arguments = extras.Call<string>("getString","arguments");
                _locationStrings[0] = arguments;
                t.text = "Arguments are::" + arguments;
            }
            else
            {
                Debug.Log("no extra");
                t.text = "NO ARGUMENTS FOUND";
            }
            */
            t.text += intent.Call<string>("getStringExtra", "latLong");
            _locationStrings[0]= intent.Call<string>("getStringExtra", "latLong");


            _locations = new Vector2d[_locationStrings.Length];
			_spawnedObjects = new List<GameObject>();
			for (int i = 0; i < _locationStrings.Length; i++)
			{
                print("Here");
                //t.text =  "Flag has been added!";
				var locationString = _locationStrings[i];
				_locations[i] = Conversions.StringToLatLon(locationString);
				var instance = Instantiate(_markerPrefab);
				instance.transform.localPosition = _map.GeoToWorldPosition(_locations[i], true);
				instance.transform.localScale = new Vector3(_spawnScale, _spawnScale, _spawnScale);
				_spawnedObjects.Add(instance);
			}
		}

		private void Update()
		{
			int count = _spawnedObjects.Count;
			for (int i = 0; i < count; i++)
			{
				var spawnedObject = _spawnedObjects[i];
				var location = _locations[i];
				spawnedObject.transform.localPosition = _map.GeoToWorldPosition(location, true);
				spawnedObject.transform.localScale = new Vector3(_spawnScale, _spawnScale, _spawnScale);
			}
		}
	}
}
 