import os
import wave
from pydub import AudioSegment
def main(fichier_audio):
    if os.path.exists(fichier_audio):
        audio = AudioSegment.from_wav(fichier_audio)
        return audio.dBFS
    else:
        print("Le fichier audio spécifié n'existe pas.")
        return "Le fichier audio spécifié n'existe pas."