import UIKit
import Flutter
import PoilabsVdNavigationUI

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
    var flutterResult: FlutterResult?
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    GeneratedPluginRegistrant.register(with: self)
    
    let controller : FlutterViewController = window?.rootViewController as! FlutterViewController
    
    let navigationChannel = FlutterMethodChannel(name: "com.poilabs/navigationChannel",
                                              binaryMessenger: controller.binaryMessenger)
    navigationChannel.setMethodCallHandler({
        (call: FlutterMethodCall, result: @escaping FlutterResult) -> Void in
        if ("start" == call.method) {
            
            let lang = Locale.current.languageCode ?? "tr"
            let _ = PoilabsVdNavigationUI(withApplicationID: "app_id", withApplicationSecret: "app_secret", withUniqueIdentifier: "ios-izmir-bb", lang: lang) { (vDcontroller) in
                controller.present(vDcontroller, animated: true, completion: nil)
            }
        } else {
            result(FlutterMethodNotImplemented)
        }
    });
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }
}
