//
//  LoginPhoneAppApp.swift
//  LoginPhoneApp
//
//  Created by Wellington Rodrigues on 21/06/26.
//

import SwiftUI

@main
struct LoginPhoneAppApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
        }
    }
}
