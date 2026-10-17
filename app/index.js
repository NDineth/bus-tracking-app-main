import { Redirect } from 'expo-router';

// Root index automatically redirects to login since the layout determines 
// if user is authenticated and redirects to home if they are.
export default function Index() {
  return <Redirect href="/login" />;
}
