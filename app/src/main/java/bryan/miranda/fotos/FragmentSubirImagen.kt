package bryan.miranda.fotos

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import modelo.ClaseConexion
import java.io.ByteArrayOutputStream
import java.sql.SQLException
import java.util.UUID

class FragmentSubirImagen : Fragment() {

    val codigo_opcion_galeria = 102
    val codigo_opcion_tomar_foto = 103
    val CAMERA_REQUEST_CODE = 0
    val STORAGE_REQUEST_CODE = 1

    lateinit var imageView: ImageView
    lateinit var miPath: String
    lateinit var txtCorreo: EditText
    lateinit var txtClave: EditText

    val uuid = UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_subir_imagen, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //1-Mandar a traer todos los elementos de la vista
        imageView = view.findViewById(R.id.imgPerfilF)
        val btnGaleria = view.findViewById<Button>(R.id.btnGaleriaF)
        val btnFoto = view.findViewById<Button>(R.id.btnFotoF)
        val txtUsuario = view.findViewById<EditText>(R.id.txtBuscarFotoUsuarioF)
        val btnBuscar = view.findViewById<Button>(R.id.btnBuscarF)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarUsuarioF)
        txtCorreo = view.findViewById(R.id.txtCorreoF)
        txtClave = view.findViewById(R.id.txtClaveF)

        btnGaleria.setOnClickListener {
            //Al darle clic al botón de la galeria pedimos los permisos primero
            checkStoragePermission()
        }

        btnFoto.setOnClickListener {
            //Al darle clic al botón de la camara pedimos los permisos primero
            checkCameraPermission()
        }

        btnGuardar.setOnClickListener {
            val correo = txtCorreo.text.toString().trim()
            val clave = txtClave.text.toString().trim()
            val imageUri = miPath

            if (correo.isNotEmpty() && clave.isNotEmpty() && imageUri != null) {
                guardarUsuarioConFoto(correo, clave, imageUri)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Completa todos los campos y selecciona una foto",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnBuscar.setOnClickListener {
            val username = txtUsuario.text.toString().trim()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //El permiso no está aceptado, entonces se lo pedimos
            pedirPermisoCamara()
        } else {
            //El permiso ya está aceptado
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, codigo_opcion_tomar_foto)
        }
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //El permiso no está aceptado, entonces se lo pedimos
            pedirPermisoAlmacenamiento()
        } else {
            //El permiso ya está aceptado
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, codigo_opcion_galeria)
        }
    }

    private fun pedirPermisoCamara() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                android.Manifest.permission.CAMERA
            )
        ) {
            //El usuario ya ha rechazado el permiso anteriormente, debemos informarle que vaya a ajustes.
        } else {
            //El usuario nunca ha aceptado ni rechazado, así que le pedimos que acepte el permiso.
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE
            )
        }
    }

    private fun pedirPermisoAlmacenamiento() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            //El usuario ya ha rechazado el permiso anteriormente, debemos informarle que vaya a ajustes.
        } else {
            //El usuario nunca ha aceptado ni rechazado, así que le pedimos que acepte el permiso.
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //El permiso está aceptado, entonces Abrimos la camara:
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(intent, codigo_opcion_tomar_foto)
                } else {
                    //El usuario ha rechazado el permiso, podemos desactivar la funcionalidad o mostrar una alerta/Toast.
                    Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
                return
            }

            STORAGE_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //El permiso está aceptado, entonces Abrimos la galeria
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.type = "image/*"
                    startActivityForResult(intent, codigo_opcion_galeria)
                } else {
                    //El usuario ha rechazado el permiso, podemos desactivar la funcionalidad o mostrar una alerta/Toast.
                    Toast.makeText(requireContext(), "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            else -> {
                // Este else lo dejamos por si sale un permiso que no teníamos controlado.
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                codigo_opcion_galeria -> {
                    val imageUri: Uri? = data?.data
                    imageUri?.let {
                        val imageBitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                        subirimagenFirebase(imageBitmap) { url ->
                            miPath = url
                            imageView.setImageURI(it)
                        }
                    }
                }

                codigo_opcion_tomar_foto -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    imageBitmap?.let {
                        subirimagenFirebase(it) { url ->
                            miPath = url
                            imageView.setImageBitmap(it)
                        }
                    }
                }
            }
        }
    }

    private fun subirimagenFirebase(bitmap: Bitmap, onSuccess: (String) -> Unit) {
        val storageRef = Firebase.storage.reference
        val imageRef = storageRef.child("images/${uuid}.jpg")
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        val uploadTask = imageRef.putBytes(data)

        uploadTask.addOnFailureListener {
            Toast.makeText(requireContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show()

        }.addOnSuccessListener { taskSnapshot ->
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }
        }
    }

    //Guardar el usuario con su correo, clave y foto (falta agregarle un UUID)
    private fun guardarUsuarioConFoto(correo: String, clave: String, imageUri: String) {
        try {
            GlobalScope.launch(Dispatchers.IO) {
                val objConexion = ClaseConexion().cadenaConexion()
                val statement =
                    objConexion?.prepareStatement("INSERT INTO tbMisUsuarios (UUID, correo, contrasena, FotoURI) VALUES (?, ?, ?, ?)")!!
                statement.setString(1, uuid)
                statement.setString(2, correo)
                statement.setString(3, clave)
                statement.setString(4, imageUri)
                statement.executeUpdate()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Datos guardados", Toast.LENGTH_SHORT).show()
                    txtCorreo.text.clear()
                    txtClave.text.clear()
                    imageView.setImageResource(0)
                    imageView.tag = null
                }
            }
        } catch (e: SQLException) {
            println("Error al guardar usuario: $e")
        }
    }

}