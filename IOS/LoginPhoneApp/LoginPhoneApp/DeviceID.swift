//
//  DeviceID.swift
//  LoginPhoneApp
//
//  Conforme o enunciado: no iOS usamos o identifierForVendor como uuid,
//  em vez de gerar um novo. Há fallback para um UUID persistido caso
//  o identifierForVendor não esteja disponível.
//

import Foundation
import UIKit

enum DeviceID {
    private static let fallbackKey = "device_uuid_fallback"

    static var current: String {
        if let vendorId = UIDevice.current.identifierForVendor?.uuidString {
            return vendorId
        }
        let defaults = UserDefaults.standard
        if let existing = defaults.string(forKey: fallbackKey) {
            return existing
        }
        let novo = UUID().uuidString
        defaults.set(novo, forKey: fallbackKey)
        return novo
    }
}
