//
//  ContentView.swift
//  LoginPhoneApp
//
//  Roteia entre as fases do app: launching -> signUp -> verification -> home.
//

import SwiftUI

struct ContentView: View {
    @EnvironmentObject var app: AppState

    var body: some View {
        Group {
            switch app.phase {
            case .launching:
                ProgressView("Carregando…")
            case .signUp:
                SignUpView()
            case .verification:
                VerificationView()
            case .home:
                HomeView()
            }
        }
        .animation(.default, value: app.phase)
        .task {
            // Auto-login na primeira aparição
            if app.phase == .launching {
                await app.bootstrap()
            }
        }
    }
}

#Preview {
    ContentView().environmentObject(AppState())
}
