package com.example.exitsw.data

import com.google.firebase.firestore.FirebaseFirestore

class PolicyRepository {

    private val db = FirebaseFirestore.getInstance()

    fun fetchPolicies(onResult: (List<Policy>) -> Unit) {
        db.collection("policies")
            .get()
            .addOnSuccessListener { result ->
                val policies = result.documents.map { doc ->
                    PolicyMapper.fromFirestore(doc.data ?: emptyMap())
                }
                onResult(policies)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}
