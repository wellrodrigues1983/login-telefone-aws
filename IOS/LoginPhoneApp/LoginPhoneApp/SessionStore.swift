//
//  SessionStore.swift
//  LoginPhoneApp
//
//  Persiste a sessão (id do usuário + telefone) para permitir auto-login.
//

import Foundation

enum SessionStore {
    private static let keyUserId = "session_user_id"
    private static let keyPhone = "session_phone"

    static func save(userId: String, phone: String) {
        let d = UserDefaults.standard
        d.set(userId, forKey: keyUserId)
        d.set(phone, forKey: keyPhone)
    }

    static var savedUserId: String? { UserDefaults.standard.string(forKey: keyUserId) }
    static var savedPhone: String? { UserDefaults.standard.string(forKey: keyPhone) }

    static var hasSession: Bool { savedUserId != nil && savedPhone != nil }

    static func clear() {
        let d = UserDefaults.standard
        d.removeObject(forKey: keyUserId)
        d.removeObject(forKey: keyPhone)
    }
}
