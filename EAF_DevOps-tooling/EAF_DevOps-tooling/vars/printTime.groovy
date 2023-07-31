#!groovy

def call(String message) {
  time = new Date().format("ddMMyy.HH:mm.ss", TimeZone.getTimeZone('Asia/Kolkata'))
  println "Timing, $message: $time"
}
