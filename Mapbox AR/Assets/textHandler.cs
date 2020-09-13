using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

public class textHandler : MonoBehaviour
{
    public InputField latLong;
    string latLongData;

    // Start is called before the first frame update
    void Start()
    {
        latLongData = null;
    }

    // Update is called once per frame
    void Update()
    {
        
    }


    public void dataReader()
    {
        if (latLong.text != null) {
            latLongData = latLong.text;
            SceneManager.LoadScene(1);
        }
        


        

    }

    private void OnDisable()
    {
        PlayerPrefs.SetString("latLongData", latLongData);
    }
}
